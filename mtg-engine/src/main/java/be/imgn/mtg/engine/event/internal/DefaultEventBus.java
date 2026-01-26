package be.imgn.mtg.engine.event.internal;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import be.imgn.mtg.engine.event.Event;
import be.imgn.mtg.engine.event.EventBus;

/// Default implementation of [EventBus].
final class DefaultEventBus implements EventBus {

    private final Map<Class<? extends Event>, List<Handler<?>>> observers = new ConcurrentHashMap<>();
    private final Map<Class<? extends Event>, List<Handler<?>>> subscribers = new ConcurrentHashMap<>();
    private final Map<Class<? extends Event>, List<Handler<?>>> notifiers = new ConcurrentHashMap<>();
    private final Map<Class<? extends Event>, List<Consumer<?>>> asyncSubscribers = new ConcurrentHashMap<>();

    private final ExecutorService asyncExecutor = Executors.newVirtualThreadPerTaskExecutor();
    private final ThreadLocal<BatchContext> batchContext = ThreadLocal.withInitial(BatchContext::new);

    @Override
    public <E extends Event> void subscribe(Class<E> eventType, Consumer<E> handler) {
        subscribe(eventType, DEFAULT_PRIORITY, handler);
    }

    @Override
    public <E extends Event> void subscribe(Class<E> eventType, int priority, Consumer<E> handler) {
        subscribers.compute(eventType, (_, existing) -> {
            var list = existing != null ? existing : new CopyOnWriteArrayList<Handler<?>>();
            list.add(new Handler<>(priority, handler));
            sortHandlers(list);
            return list;
        });
    }

    @Override
    public <E extends Event> void observe(Class<E> eventType, Consumer<E> handler) {
        observers
                .computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>())
                .add(new Handler<>(DEFAULT_PRIORITY, handler));
    }

    @Override
    public <E extends Event> void notify(Class<E> eventType, Consumer<E> handler) {
        notifiers
                .computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>())
                .add(new Handler<>(DEFAULT_PRIORITY, handler));
    }

    @Override
    public <E extends Event> void subscribeAsync(Class<E> eventType, Consumer<E> handler) {
        asyncSubscribers
                .computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>())
                .add(handler);
    }

    @Override
    public void beginBatch() {
        batchContext.get().increment();
    }

    @Override
    public void endBatch() {
        var context = batchContext.get();
        if (context.decrement() == 0) {
            var pending = context.drainPending();
            for (var event : pending) {
                dispatchEvent(event);
            }
        }
    }

    @Override
    public Batch batch() {
        beginBatch();
        return this::endBatch;
    }

    @Override
    public void post(Event event) {
        var context = batchContext.get();
        if (context.isInBatch()) {
            context.addPending(event);
        } else {
            dispatchEvent(event);
        }
    }

    @Override
    public void shutdown() {
        asyncExecutor.shutdown();
    }

    private void dispatchEvent(Event event) {
        var eventType = event.getClass();

        // Phase 1: Observers (with hierarchical matching)
        dispatchToObservers(eventType, event);

        // Phase 2: Subscribers (sorted by priority, exact type matching)
        var subscriberHandlers = subscribers.get(eventType);
        if (subscriberHandlers != null) {
            dispatchToHandlers(subscriberHandlers, event);
        }

        // Phase 3: Async subscribers (exact type matching)
        var asyncHandlers = asyncSubscribers.get(eventType);
        if (asyncHandlers != null) {
            for (var handler : asyncHandlers) {
                asyncExecutor.execute(() -> invokeHandler(handler, event));
            }
        }

        // Phase 4: Notifiers (exact type matching)
        var notifierHandlers = notifiers.get(eventType);
        if (notifierHandlers != null) {
            dispatchToHandlers(notifierHandlers, event);
        }
    }

    private void dispatchToObservers(Class<?> eventType, Event event) {
        // Walk the class hierarchy to support polymorphic observers
        var currentType = eventType;
        while (currentType != null && Event.class.isAssignableFrom(currentType)) {
            var observerHandlers = observers.get(currentType);
            if (observerHandlers != null) {
                dispatchToHandlers(observerHandlers, event);
            }
            // Also check interfaces
            for (var iface : currentType.getInterfaces()) {
                if (Event.class.isAssignableFrom(iface)) {
                    var ifaceHandlers = observers.get(iface);
                    if (ifaceHandlers != null) {
                        dispatchToHandlers(ifaceHandlers, event);
                    }
                }
            }
            currentType = currentType.getSuperclass();
        }
    }

    private void dispatchToHandlers(List<Handler<?>> handlers, Event event) {
        for (var handler : handlers) {
            invokeHandler(handler.consumer(), event);
        }
    }

    @SuppressWarnings("unchecked")
    private <E extends Event> void invokeHandler(Consumer<?> handler, E event) {
        ((Consumer<E>) handler).accept(event);
    }

    private void sortHandlers(List<Handler<?>> handlers) {
        if (handlers instanceof CopyOnWriteArrayList<Handler<?>> cowList) {
            var sorted = new ArrayList<>(cowList);
            sorted.sort(Comparator.comparingInt(Handler::priority));
            cowList.clear();
            cowList.addAll(sorted);
        }
    }

    private record Handler<E extends Event>(int priority, Consumer<E> consumer) {}

    private static final class BatchContext {
        private final AtomicInteger depth = new AtomicInteger(0);
        private final List<Event> pending = new ArrayList<>();

        void increment() {
            depth.incrementAndGet();
        }

        int decrement() {
            return depth.decrementAndGet();
        }

        boolean isInBatch() {
            return depth.get() > 0;
        }

        void addPending(Event event) {
            pending.add(event);
        }

        List<Event> drainPending() {
            var result = new ArrayList<>(pending);
            pending.clear();
            return result;
        }
    }
}
