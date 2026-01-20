package be.imgn.mtg.engine.event.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.event.Event;
import be.imgn.mtg.engine.event.EventBus;
import be.imgn.mtg.engine.event.EventTracker;
import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.turn.TurnStartedEvent;

class EventTrackerTest {

    private EventBus eventBus;
    private EventTracker eventTracker;
    private Player mockPlayer;

    @BeforeEach
    void setUp() {
        eventBus = new DefaultEventBus();
        eventTracker = new DefaultEventTracker(eventBus);
        mockPlayer = mock(Player.class);
    }

    @AfterEach
    void tearDown() {
        eventBus.shutdown();
    }

    // Test events
    record TestEvent(String message) implements Event {}

    record AnotherEvent(int value) implements Event {}

    @Nested
    class EventsFromThisTurn {

        @Test
        void emptyWhenNoEventsPosted() {
            var events = eventTracker.eventsFromThisTurn().toList();

            assertThat(events).isEmpty();
        }

        @Test
        void tracksPostedEvents() {
            eventBus.post(new TestEvent("first"));
            eventBus.post(new TestEvent("second"));

            var events = eventTracker.eventsFromThisTurn().toList();

            assertThat(events).hasSize(2);
        }

        @Test
        void filtersEventsByType() {
            eventBus.post(new TestEvent("test"));
            eventBus.post(new AnotherEvent(42));
            eventBus.post(new TestEvent("test2"));

            var testEvents = eventTracker.eventsFromThisTurn(TestEvent.class).toList();
            var anotherEvents =
                    eventTracker.eventsFromThisTurn(AnotherEvent.class).toList();

            assertThat(testEvents).hasSize(2);
            assertThat(anotherEvents).hasSize(1);
        }

        @Test
        void preservesEventOrder() {
            eventBus.post(new TestEvent("first"));
            eventBus.post(new TestEvent("second"));
            eventBus.post(new TestEvent("third"));

            var events = eventTracker.eventsFromThisTurn(TestEvent.class).toList();

            assertThat(events).extracting(TestEvent::message).containsExactly("first", "second", "third");
        }
    }

    @Nested
    class TurnTransitions {

        @Test
        void turnStartedEventUpdatesCurrentTurn() {
            eventBus.post(new TestEvent("turn0"));
            eventBus.post(new TurnStartedEvent(1, mockPlayer));
            eventBus.post(new TestEvent("turn1"));

            var eventsThisTurn = eventTracker.eventsFromThisTurn().toList();

            // Should include TurnStartedEvent and TestEvent from turn 1
            assertThat(eventsThisTurn).hasSize(2);
            assertThat(eventsThisTurn)
                    .filteredOn(TestEvent.class::isInstance)
                    .extracting(e -> ((TestEvent) e).message())
                    .containsExactly("turn1");
        }

        @Test
        void previousTurnEventsAccessible() {
            eventBus.post(new TestEvent("turn0-event1"));
            eventBus.post(new TestEvent("turn0-event2"));
            eventBus.post(new TurnStartedEvent(1, mockPlayer));
            eventBus.post(new TestEvent("turn1-event"));

            var previousTurnEvents = eventTracker.eventsFromPreviousTurn().toList();

            assertThat(previousTurnEvents).hasSize(2);
            assertThat(previousTurnEvents)
                    .filteredOn(TestEvent.class::isInstance)
                    .extracting(e -> ((TestEvent) e).message())
                    .containsExactly("turn0-event1", "turn0-event2");
        }

        @Test
        void previousTurnEmptyOnFirstTurn() {
            eventBus.post(new TestEvent("turn0-event"));

            var previousTurnEvents = eventTracker.eventsFromPreviousTurn().toList();

            assertThat(previousTurnEvents).isEmpty();
        }

        @Test
        void multipleTurnTransitions() {
            // Turn 0
            eventBus.post(new TestEvent("t0"));

            // Turn 1
            eventBus.post(new TurnStartedEvent(1, mockPlayer));
            eventBus.post(new TestEvent("t1-a"));
            eventBus.post(new TestEvent("t1-b"));

            // Turn 2
            eventBus.post(new TurnStartedEvent(2, mockPlayer));
            eventBus.post(new TestEvent("t2"));

            var currentTurnEvents =
                    eventTracker.eventsFromThisTurn(TestEvent.class).toList();
            var previousTurnEvents =
                    eventTracker.eventsFromPreviousTurn(TestEvent.class).toList();

            assertThat(currentTurnEvents).extracting(TestEvent::message).containsExactly("t2");

            assertThat(previousTurnEvents).extracting(TestEvent::message).containsExactly("t1-a", "t1-b");
        }

        @Test
        void turnStartedEventIsTracked() {
            eventBus.post(new TurnStartedEvent(1, mockPlayer));

            var turnStartEvents =
                    eventTracker.eventsFromThisTurn(TurnStartedEvent.class).toList();

            assertThat(turnStartEvents).hasSize(1);
            assertThat(turnStartEvents.getFirst().turnNumber()).isEqualTo(1);
        }
    }

    @Nested
    class TypeFiltering {

        @Test
        void filteringByExactType() {
            eventBus.post(new TestEvent("test"));
            eventBus.post(new AnotherEvent(1));

            assertThat(eventTracker.eventsFromThisTurn(TestEvent.class).count()).isEqualTo(1);
            assertThat(eventTracker.eventsFromThisTurn(AnotherEvent.class).count())
                    .isEqualTo(1);
        }

        @Test
        void filteringByTurnStartedEvent() {
            eventBus.post(new TurnStartedEvent(1, mockPlayer));

            assertThat(eventTracker.eventsFromThisTurn(TurnStartedEvent.class).count())
                    .isEqualTo(1);
        }

        @Test
        void filteringByBaseType() {
            eventBus.post(new TestEvent("test"));
            eventBus.post(new AnotherEvent(1));

            // Event.class should match all events
            var allEvents = eventTracker.eventsFromThisTurn(Event.class).toList();

            assertThat(allEvents).hasSize(2);
        }

        @Test
        void filteringReturnsEmptyForNoMatches() {
            eventBus.post(new TestEvent("test"));

            var noMatches = eventTracker.eventsFromThisTurn(AnotherEvent.class).toList();

            assertThat(noMatches).isEmpty();
        }
    }

    @Nested
    class Integration {

        @Test
        void trackerWorksWithBatchedEvents() {
            try (var ignored = eventBus.batch()) {
                eventBus.post(new TestEvent("batched1"));
                eventBus.post(new TestEvent("batched2"));
            }

            var events = eventTracker.eventsFromThisTurn(TestEvent.class).toList();

            assertThat(events).hasSize(2);
        }

        @Test
        void trackerReceivesEventsBeforeSubscribers() {
            // Tracker uses observe(), so it should see events before subscribers
            var trackerSawEvent = new boolean[] {false};

            eventBus.subscribe(TestEvent.class, _ -> {
                // At this point, the tracker should have already recorded the event
                trackerSawEvent[0] =
                        eventTracker.eventsFromThisTurn(TestEvent.class).count() > 0;
            });

            eventBus.post(new TestEvent("test"));

            assertThat(trackerSawEvent[0]).isTrue();
        }

        @Test
        void multipleEventsInSameTurnMaintainOrder() {
            for (int i = 0; i < 100; i++) {
                eventBus.post(new AnotherEvent(i));
            }

            var events = eventTracker.eventsFromThisTurn(AnotherEvent.class).toList();

            assertThat(events).hasSize(100);
            for (int i = 0; i < 100; i++) {
                assertThat(events.get(i).value()).isEqualTo(i);
            }
        }
    }
}
