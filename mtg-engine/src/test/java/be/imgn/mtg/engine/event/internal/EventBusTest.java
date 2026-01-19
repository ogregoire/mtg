package be.imgn.mtg.engine.event.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.event.Event;
import be.imgn.mtg.engine.event.EventBus;

class EventBusTest {

    private EventBus eventBus;

    @BeforeEach
    void setUp() {
        eventBus = new DefaultEventBus();
    }

    @AfterEach
    void tearDown() {
        eventBus.shutdown();
    }

    // Test events
    record TestEvent(String message) implements Event {}

    record AnotherEvent(int value) implements Event {}

    @Nested
    class BasicSubscription {

        @Test
        void subscriberReceivesPostedEvent() {
            var received = new ArrayList<TestEvent>();

            eventBus.subscribe(TestEvent.class, received::add);
            eventBus.post(new TestEvent("hello"));

            assertThat(received).hasSize(1);
            assertThat(received.getFirst().message()).isEqualTo("hello");
        }

        @Test
        void multipleSubscribersReceiveEvent() {
            var received1 = new ArrayList<TestEvent>();
            var received2 = new ArrayList<TestEvent>();

            eventBus.subscribe(TestEvent.class, received1::add);
            eventBus.subscribe(TestEvent.class, received2::add);
            eventBus.post(new TestEvent("test"));

            assertThat(received1).hasSize(1);
            assertThat(received2).hasSize(1);
        }

        @Test
        void subscriberOnlyReceivesMatchingEventType() {
            var testEvents = new ArrayList<TestEvent>();
            var anotherEvents = new ArrayList<AnotherEvent>();

            eventBus.subscribe(TestEvent.class, testEvents::add);
            eventBus.subscribe(AnotherEvent.class, anotherEvents::add);

            eventBus.post(new TestEvent("test"));
            eventBus.post(new AnotherEvent(42));

            assertThat(testEvents).hasSize(1);
            assertThat(anotherEvents).hasSize(1);
        }

        @Test
        void noSubscribersDoesNotThrow() {
            // Should not throw
            eventBus.post(new TestEvent("no subscribers"));
        }
    }

    @Nested
    class ObserverPhase {

        @Test
        void observerReceivesEvent() {
            var received = new ArrayList<TestEvent>();

            eventBus.observe(TestEvent.class, received::add);
            eventBus.post(new TestEvent("observed"));

            assertThat(received).hasSize(1);
        }

        @Test
        void multipleObserversReceiveEvent() {
            var received1 = new ArrayList<TestEvent>();
            var received2 = new ArrayList<TestEvent>();

            eventBus.observe(TestEvent.class, received1::add);
            eventBus.observe(TestEvent.class, received2::add);
            eventBus.post(new TestEvent("test"));

            assertThat(received1).hasSize(1);
            assertThat(received2).hasSize(1);
        }
    }

    @Nested
    class NotifierPhase {

        @Test
        void notifierReceivesEvent() {
            var received = new ArrayList<TestEvent>();

            eventBus.notify(TestEvent.class, received::add);
            eventBus.post(new TestEvent("notified"));

            assertThat(received).hasSize(1);
        }

        @Test
        void multipleNotifiersReceiveEvent() {
            var received1 = new ArrayList<TestEvent>();
            var received2 = new ArrayList<TestEvent>();

            eventBus.notify(TestEvent.class, received1::add);
            eventBus.notify(TestEvent.class, received2::add);
            eventBus.post(new TestEvent("test"));

            assertThat(received1).hasSize(1);
            assertThat(received2).hasSize(1);
        }
    }

    @Nested
    class PhaseOrdering {

        @Test
        void observersCalledBeforeSubscribers() {
            var order = new ArrayList<String>();

            eventBus.subscribe(TestEvent.class, _ -> order.add("subscriber"));
            eventBus.observe(TestEvent.class, _ -> order.add("observer"));

            eventBus.post(new TestEvent("test"));

            assertThat(order).containsExactly("observer", "subscriber");
        }

        @Test
        void subscribersCalledBeforeNotifiers() {
            var order = new ArrayList<String>();

            eventBus.notify(TestEvent.class, _ -> order.add("notifier"));
            eventBus.subscribe(TestEvent.class, _ -> order.add("subscriber"));

            eventBus.post(new TestEvent("test"));

            assertThat(order).containsExactly("subscriber", "notifier");
        }

        @Test
        void fullPhaseOrdering() {
            var order = new ArrayList<String>();

            eventBus.notify(TestEvent.class, _ -> order.add("notifier"));
            eventBus.subscribe(TestEvent.class, _ -> order.add("subscriber"));
            eventBus.observe(TestEvent.class, _ -> order.add("observer"));

            eventBus.post(new TestEvent("test"));

            assertThat(order).containsExactly("observer", "subscriber", "notifier");
        }
    }

    @Nested
    class SubscriberPriority {

        @Test
        void higherPriorityCalledFirst() {
            var order = new ArrayList<String>();

            eventBus.subscribe(TestEvent.class, EventBus.LOW_PRIORITY, _ -> order.add("low"));
            eventBus.subscribe(TestEvent.class, EventBus.HIGH_PRIORITY, _ -> order.add("high"));
            eventBus.subscribe(TestEvent.class, EventBus.DEFAULT_PRIORITY, _ -> order.add("default"));

            eventBus.post(new TestEvent("test"));

            assertThat(order).containsExactly("high", "default", "low");
        }

        @Test
        void customPrioritiesRespected() {
            var order = new ArrayList<String>();

            eventBus.subscribe(TestEvent.class, 50, _ -> order.add("50"));
            eventBus.subscribe(TestEvent.class, 25, _ -> order.add("25"));
            eventBus.subscribe(TestEvent.class, 75, _ -> order.add("75"));
            eventBus.subscribe(TestEvent.class, 10, _ -> order.add("10"));

            eventBus.post(new TestEvent("test"));

            assertThat(order).containsExactly("10", "25", "50", "75");
        }

        @Test
        void samePriorityPreservesRegistrationOrder() {
            var order = new ArrayList<String>();

            eventBus.subscribe(TestEvent.class, 50, _ -> order.add("first"));
            eventBus.subscribe(TestEvent.class, 50, _ -> order.add("second"));
            eventBus.subscribe(TestEvent.class, 50, _ -> order.add("third"));

            eventBus.post(new TestEvent("test"));

            // Same priority should maintain insertion order (stable sort)
            assertThat(order).containsExactly("first", "second", "third");
        }
    }

    @Nested
    class AsyncSubscription {

        @Test
        void asyncSubscriberReceivesEvent() throws InterruptedException {
            var latch = new CountDownLatch(1);
            var received = new CopyOnWriteArrayList<TestEvent>();

            eventBus.subscribeAsync(TestEvent.class, event -> {
                received.add(event);
                latch.countDown();
            });

            eventBus.post(new TestEvent("async"));

            assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
            assertThat(received).hasSize(1);
        }

        @Test
        void multipleAsyncSubscribersExecuteInParallel() throws InterruptedException {
            var threadNames = new CopyOnWriteArrayList<String>();
            var latch = new CountDownLatch(3);

            for (int i = 0; i < 3; i++) {
                eventBus.subscribeAsync(TestEvent.class, _ -> {
                    threadNames.add(Thread.currentThread().getName());
                    latch.countDown();
                });
            }

            eventBus.post(new TestEvent("parallel"));

            assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
            assertThat(threadNames).hasSize(3);
        }

        @Test
        void asyncSubscribersDoNotBlockSynchronousHandlers() throws InterruptedException {
            var order = Collections.synchronizedList(new ArrayList<String>());
            var asyncStarted = new CountDownLatch(1);
            var asyncComplete = new CountDownLatch(1);

            eventBus.subscribeAsync(TestEvent.class, _ -> {
                asyncStarted.countDown();
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                order.add("async");
                asyncComplete.countDown();
            });

            eventBus.notify(TestEvent.class, _ -> order.add("notifier"));

            eventBus.post(new TestEvent("test"));

            // Notifier should complete before async
            assertThat(order).contains("notifier");

            // Wait for async to complete
            assertThat(asyncComplete.await(5, TimeUnit.SECONDS)).isTrue();
            assertThat(order).contains("async");
        }
    }

    @Nested
    class Batching {

        @Test
        void batchHoldsEvents() {
            var received = new ArrayList<TestEvent>();
            eventBus.subscribe(TestEvent.class, received::add);

            eventBus.beginBatch();
            eventBus.post(new TestEvent("first"));
            eventBus.post(new TestEvent("second"));

            assertThat(received).isEmpty();

            eventBus.endBatch();

            assertThat(received).hasSize(2);
        }

        @Test
        void batchWithTryWithResources() {
            var received = new ArrayList<TestEvent>();
            eventBus.subscribe(TestEvent.class, received::add);

            try (var ignored = eventBus.batch()) {
                eventBus.post(new TestEvent("first"));
                eventBus.post(new TestEvent("second"));
                assertThat(received).isEmpty();
            }

            assertThat(received).hasSize(2);
        }

        @Test
        void nestedBatchesOnlyDispatchOnOuterEnd() {
            var received = new ArrayList<TestEvent>();
            eventBus.subscribe(TestEvent.class, received::add);

            eventBus.beginBatch();
            eventBus.post(new TestEvent("outer1"));

            eventBus.beginBatch();
            eventBus.post(new TestEvent("inner"));
            eventBus.endBatch();

            assertThat(received).isEmpty();

            eventBus.post(new TestEvent("outer2"));
            eventBus.endBatch();

            assertThat(received).hasSize(3);
        }

        @Test
        void nestedBatchesWithTryWithResources() {
            var received = new ArrayList<TestEvent>();
            eventBus.subscribe(TestEvent.class, received::add);

            try (var outer = eventBus.batch()) {
                eventBus.post(new TestEvent("outer1"));

                try (var inner = eventBus.batch()) {
                    eventBus.post(new TestEvent("inner"));
                }

                assertThat(received).isEmpty();
                eventBus.post(new TestEvent("outer2"));
            }

            assertThat(received).hasSize(3);
        }

        @Test
        void eventsDispatchedInOrderAfterBatch() {
            var messages = new ArrayList<String>();
            eventBus.subscribe(TestEvent.class, e -> messages.add(e.message()));

            try (var ignored = eventBus.batch()) {
                eventBus.post(new TestEvent("first"));
                eventBus.post(new TestEvent("second"));
                eventBus.post(new TestEvent("third"));
            }

            assertThat(messages).containsExactly("first", "second", "third");
        }
    }

    @Nested
    class ConcurrencyTests {

        @RepeatedTest(10)
        void concurrentPostsAreThreadSafe() throws InterruptedException {
            var received = new CopyOnWriteArrayList<TestEvent>();
            eventBus.subscribe(TestEvent.class, received::add);

            var threads = 10;
            var eventsPerThread = 100;
            var latch = new CountDownLatch(threads);

            try (var executor = Executors.newFixedThreadPool(threads)) {
                for (int t = 0; t < threads; t++) {
                    final int threadId = t;
                    executor.execute(() -> {
                        for (int i = 0; i < eventsPerThread; i++) {
                            eventBus.post(new TestEvent("thread-" + threadId + "-event-" + i));
                        }
                        latch.countDown();
                    });
                }

                assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
            }

            assertThat(received).hasSize(threads * eventsPerThread);
        }

        @RepeatedTest(10)
        void concurrentSubscriptionsAreThreadSafe() throws InterruptedException {
            var subscriberCount = new AtomicInteger(0);
            var threads = 10;
            var barrier = new CyclicBarrier(threads);
            var subscriptionLatch = new CountDownLatch(threads);

            try (var executor = Executors.newFixedThreadPool(threads)) {
                for (int t = 0; t < threads; t++) {
                    executor.execute(() -> {
                        try {
                            barrier.await();
                            eventBus.subscribe(TestEvent.class, _ -> subscriberCount.incrementAndGet());
                            subscriptionLatch.countDown();
                        } catch (Exception e) {
                            Thread.currentThread().interrupt();
                        }
                    });
                }

                assertThat(subscriptionLatch.await(10, TimeUnit.SECONDS)).isTrue();
            }

            eventBus.post(new TestEvent("test"));

            // All subscriptions should have been registered
            assertThat(subscriberCount.get()).isEqualTo(threads);
        }

        @RepeatedTest(10)
        void batchContextIsThreadLocal() throws InterruptedException {
            var received = new CopyOnWriteArrayList<String>();
            eventBus.subscribe(TestEvent.class, e -> received.add(e.message()));

            var threads = 5;
            var barrier = new CyclicBarrier(threads);
            var latch = new CountDownLatch(threads);

            try (var executor = Executors.newFixedThreadPool(threads)) {
                for (int t = 0; t < threads; t++) {
                    final int threadId = t;
                    executor.execute(() -> {
                        try {
                            barrier.await();

                            // Each thread has its own batch context
                            try (var ignored = eventBus.batch()) {
                                eventBus.post(new TestEvent("batched-" + threadId));
                                // Small delay to ensure overlap
                                Thread.sleep(10);
                            }
                            // Event should be dispatched immediately after this thread's batch ends

                            latch.countDown();
                        } catch (Exception e) {
                            Thread.currentThread().interrupt();
                        }
                    });
                }

                assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
            }

            // All threads should have dispatched their events
            assertThat(received).hasSize(threads);
        }

        @RepeatedTest(5)
        void highContention() throws InterruptedException {
            var received = new CopyOnWriteArrayList<TestEvent>();
            var asyncReceived = new CopyOnWriteArrayList<TestEvent>();
            var observerCount = new AtomicInteger(0);
            var notifierCount = new AtomicInteger(0);

            eventBus.observe(TestEvent.class, _ -> observerCount.incrementAndGet());
            eventBus.subscribe(TestEvent.class, received::add);
            eventBus.subscribeAsync(TestEvent.class, asyncReceived::add);
            eventBus.notify(TestEvent.class, _ -> notifierCount.incrementAndGet());

            var threads = 20;
            var eventsPerThread = 50;
            var latch = new CountDownLatch(threads);
            var barrier = new CyclicBarrier(threads);

            try (var executor = Executors.newFixedThreadPool(threads)) {
                for (int t = 0; t < threads; t++) {
                    final int threadId = t;
                    executor.execute(() -> {
                        try {
                            barrier.await();
                            for (int i = 0; i < eventsPerThread; i++) {
                                eventBus.post(new TestEvent("t" + threadId + "-e" + i));
                            }
                            latch.countDown();
                        } catch (Exception e) {
                            Thread.currentThread().interrupt();
                        }
                    });
                }

                assertThat(latch.await(30, TimeUnit.SECONDS)).isTrue();
            }

            var totalEvents = threads * eventsPerThread;
            assertThat(received).hasSize(totalEvents);
            assertThat(observerCount.get()).isEqualTo(totalEvents);
            assertThat(notifierCount.get()).isEqualTo(totalEvents);

            // Wait for async handlers
            Thread.sleep(500);
            assertThat(asyncReceived).hasSize(totalEvents);
        }

        @Test
        void subscribeDuringEventDispatch() {
            var received = new ArrayList<String>();

            eventBus.subscribe(TestEvent.class, _ -> {
                received.add("first");
                // Subscribe another handler during dispatch
                eventBus.subscribe(TestEvent.class, _ -> received.add("dynamic"));
            });

            eventBus.post(new TestEvent("test1"));
            assertThat(received).containsExactly("first");

            received.clear();
            eventBus.post(new TestEvent("test2"));
            // Now the dynamically added handler should also be called
            assertThat(received).containsExactly("first", "dynamic");
        }
    }

    @Nested
    class EdgeCases {

        @Test
        void emptyBatchDoesNothing() {
            var received = new ArrayList<TestEvent>();
            eventBus.subscribe(TestEvent.class, received::add);

            eventBus.beginBatch();
            eventBus.endBatch();

            assertThat(received).isEmpty();
        }

        @Test
        void emptyBatchWithTryWithResources() {
            var received = new ArrayList<TestEvent>();
            eventBus.subscribe(TestEvent.class, received::add);

            try (var _ = eventBus.batch()) {
                // Post nothing
            }

            assertThat(received).isEmpty();
        }

        @Test
        void eventCanBePostedFromHandler() {
            var received = new ArrayList<String>();

            eventBus.subscribe(TestEvent.class, e -> {
                received.add(e.message());
                if (e.message().equals("trigger")) {
                    eventBus.post(new TestEvent("triggered"));
                }
            });

            eventBus.post(new TestEvent("trigger"));

            assertThat(received).containsExactly("trigger", "triggered");
        }

        @Test
        void batchedEventCanTriggerMoreEvents() {
            var received = new ArrayList<String>();

            eventBus.subscribe(TestEvent.class, e -> {
                received.add(e.message());
                if (e.message().equals("trigger")) {
                    eventBus.post(new TestEvent("triggered"));
                }
            });

            try (var ignored = eventBus.batch()) {
                eventBus.post(new TestEvent("trigger"));
            }

            assertThat(received).containsExactly("trigger", "triggered");
        }

        @Test
        void differentEventTypesInSameBatch() {
            var testEvents = new ArrayList<TestEvent>();
            var anotherEvents = new ArrayList<AnotherEvent>();

            eventBus.subscribe(TestEvent.class, testEvents::add);
            eventBus.subscribe(AnotherEvent.class, anotherEvents::add);

            try (var ignored = eventBus.batch()) {
                eventBus.post(new TestEvent("test"));
                eventBus.post(new AnotherEvent(42));
                eventBus.post(new TestEvent("test2"));
            }

            assertThat(testEvents).hasSize(2);
            assertThat(anotherEvents).hasSize(1);
        }
    }

    @Nested
    class Shutdown {

        @Test
        void shutdownCompletesGracefully() {
            eventBus.subscribeAsync(TestEvent.class, _ -> {});
            eventBus.post(new TestEvent("test"));
            eventBus.shutdown();
            // Should not throw
        }
    }
}
