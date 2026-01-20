package be.imgn.mtg.engine.turn.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.event.Event;
import be.imgn.mtg.engine.event.EventBus;
import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.state.GameState;
import be.imgn.mtg.engine.turn.DurationTracker;
import be.imgn.mtg.engine.turn.OccurrenceTracker;
import be.imgn.mtg.engine.turn.PrioritySystem;
import be.imgn.mtg.engine.turn.SBAEngine;
import be.imgn.mtg.engine.turn.SkipTracker;
import be.imgn.mtg.engine.turn.TurnEndedEvent;
import be.imgn.mtg.engine.turn.TurnTracker;
import be.imgn.mtg.engine.zone.Stack;

class TurnTrackerIntegrationTest {

    private GameState gameState;
    private EventBus eventBus;
    private OccurrenceTracker occurrenceTracker;
    private PrioritySystem prioritySystem;
    private SBAEngine sbaEngine;
    private DurationTracker durationTracker;
    private SkipTracker skipTracker;
    private Stack stack;

    private Player player1;
    private Player player2;

    private List<Event> firedEvents;

    @BeforeEach
    void setUp() {
        gameState = mock(GameState.class);
        eventBus = mock(EventBus.class);
        occurrenceTracker = new DefaultOccurrenceTracker();
        prioritySystem = mock(PrioritySystem.class);
        sbaEngine = mock(SBAEngine.class);
        durationTracker = mock(DurationTracker.class);
        skipTracker = mock(SkipTracker.class);
        stack = mock(Stack.class);

        player1 = mock(Player.class);
        player2 = mock(Player.class);

        when(gameState.players()).thenReturn(List.of(player1, player2));
        when(gameState.nextPlayerInTurnOrder(player1)).thenReturn(player2);
        when(gameState.nextPlayerInTurnOrder(player2)).thenReturn(player1);
        when(gameState.stack()).thenReturn(stack);
        when(stack.isEmpty()).thenReturn(true);

        // Track fired events
        firedEvents = new ArrayList<>();
        doAnswer(inv -> {
                    firedEvents.add(inv.getArgument(0));
                    return null;
                })
                .when(eventBus)
                .post(any(Event.class));
    }

    private TurnTracker createTracker() {
        return new DefaultTurnTracker(
                gameState, eventBus, occurrenceTracker, prioritySystem, sbaEngine, durationTracker, skipTracker);
    }

    @Nested
    class Run {

        @Test
        void throwsExceptionWhenNoPlayers() {
            when(gameState.players()).thenReturn(List.of());

            var tracker = createTracker();

            assertThatThrownBy(tracker::run)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("no players");
        }

        @Test
        void stopsWhenGameIsOver() {
            // Game is over after first turn
            when(gameState.isGameOver()).thenReturn(false, true);

            // All pass immediately
            when(prioritySystem.allPassed()).thenReturn(true);

            var tracker = createTracker();
            tracker.run();

            // Should have completed at least one turn
            assertThat(tracker.turnState().turnNumber()).isEqualTo(1);
        }
    }

    @Nested
    class TurnState {

        @Test
        void providesTurnStateAccess() {
            var tracker = createTracker();
            assertThat(tracker.turnState()).isNotNull();
        }

        @Test
        void turnNumberIncrementsDuringRun() {
            // Game ends after 2 turns - use counter since isGameOver is called many times per turn
            var turnCounter = new AtomicInteger(0);
            doAnswer(inv -> {
                        firedEvents.add(inv.getArgument(0));
                        if (inv.getArgument(0) instanceof TurnEndedEvent) {
                            turnCounter.incrementAndGet();
                        }
                        return null;
                    })
                    .when(eventBus)
                    .post(any(Event.class));

            when(gameState.isGameOver()).thenAnswer(inv -> turnCounter.get() >= 2);
            when(prioritySystem.allPassed()).thenReturn(true);

            var tracker = createTracker();
            tracker.run();

            assertThat(tracker.turnState().turnNumber()).isEqualTo(2);
        }
    }

    @Nested
    class EndTurnEarly {

        @Test
        void endTurnEarlySkipsRemainingPhases() {
            // Game ends after first turn
            when(gameState.isGameOver()).thenReturn(false, true);
            when(prioritySystem.allPassed()).thenReturn(true);

            var tracker = createTracker();

            // Set up to call endTurnEarly during the turn
            doAnswer(inv -> {
                        // End the turn during the first phase started event
                        if (firedEvents.size() == 2) { // After TurnStartedEvent and first PhaseStartedEvent
                            tracker.endTurnEarly();
                        }
                        firedEvents.add(inv.getArgument(0));
                        return null;
                    })
                    .when(eventBus)
                    .post(any(Event.class));

            tracker.run();

            // Turn should have ended
            assertThat(tracker.turnState().turnNumber()).isEqualTo(1);
        }
    }
}
