package be.imgn.mtg.engine.event;

import java.util.function.Consumer;

/// Central event dispatcher for the MTG engine.
///
/// Events are dispatched in three phases:
///
/// 1. **Observe** - Tools that observe when an event is fired
/// 2. **Subscribe** - Normal synchronous handlers
/// 3. **Notify** - UI notifications after all processing is done
///
/// Events can be batched using [#beginBatch()]/[#endBatch()] or the try-with-resources
/// pattern with [#batch()]. Batched events are held until the batch context ends.
public interface EventBus {

    /// Default priority for subscribers.
    int DEFAULT_PRIORITY = 100;

    /// High priority (executed first).
    int HIGH_PRIORITY = 0;

    /// Low priority (executed last).
    int LOW_PRIORITY = 200;

    /// Subscribes to events of the given type with default priority.
    ///
    /// @param eventType the event class to subscribe to
    /// @param handler the handler to invoke when the event is posted
    /// @param <E> the event type
    <E extends Event> void subscribe(Class<E> eventType, Consumer<E> handler);

    /// Subscribes to events of the given type with specified priority.
    ///
    /// @param eventType the event class to subscribe to
    /// @param priority lower values execute first
    /// @param handler the handler to invoke when the event is posted
    /// @param <E> the event type
    <E extends Event> void subscribe(Class<E> eventType, int priority, Consumer<E> handler);

    /// Registers an observer for events. Observers are called before subscribers.
    ///
    /// @param eventType the event class to observe
    /// @param handler the handler to invoke when the event is posted
    /// @param <E> the event type
    <E extends Event> void observe(Class<E> eventType, Consumer<E> handler);

    /// Registers a notification handler. Notifications are called after all subscribers.
    ///
    /// @param eventType the event class to be notified of
    /// @param handler the handler to invoke when the event is posted
    /// @param <E> the event type
    <E extends Event> void notify(Class<E> eventType, Consumer<E> handler);

    /// Subscribes to events asynchronously.
    ///
    /// @param eventType the event class to subscribe to
    /// @param handler the handler to invoke when the event is posted
    /// @param <E> the event type
    <E extends Event> void subscribeAsync(Class<E> eventType, Consumer<E> handler);

    /// Begins a batch context. Events posted will be held until [#endBatch()] is called.
    void beginBatch();

    /// Ends a batch context and dispatches any pending events.
    void endBatch();

    /// Creates a batch context for use in try-with-resources.
    ///
    /// ```java
    /// try (var batch = eventBus.batch()) {
    ///     eventBus.post(event1);
    ///     eventBus.post(event2);
    /// } // events dispatched here
    /// ```
    ///
    /// @return a batch context that calls [#endBatch()] when closed
    Batch batch();

    /// Posts an event to all registered handlers.
    ///
    /// @param event the event to post
    void post(Event event);

    /// Shuts down the event bus, releasing any resources.
    void shutdown();

    /// A batch context for use with try-with-resources.
    interface Batch extends AutoCloseable {
        @Override
        void close();
    }
}
