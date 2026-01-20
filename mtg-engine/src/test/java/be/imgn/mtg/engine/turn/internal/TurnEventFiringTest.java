package be.imgn.mtg.engine.turn.internal;

import static org.assertj.core.api.Assertions.assertThat;
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
import be.imgn.mtg.engine.turn.TurnStartedEvent;
import be.imgn.mtg.engine.zone.Stack;

class TurnEventFiringTest {

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
        when(prioritySystem.allPassed()).thenReturn(true);

        firedEvents = new ArrayList<>();
        doAnswer(inv -> {
                    firedEvents.add(inv.getArgument(0));
                    return null;
                })
                .when(eventBus)
                .post(any(Event.class));
    }

    private DefaultTurnTracker createTracker() {
        return new DefaultTurnTracker(
                gameState, eventBus, occurrenceTracker, prioritySystem, sbaEngine, durationTracker, skipTracker);
    }

    @Nested
    class TurnStartedEventFiring {

        @Test
        void firesTurnStartedEventAtStartOfTurn() {
            // Game ends after first turn
            when(gameState.isGameOver()).thenReturn(false, true);

            createTracker().run();

            List<TurnStartedEvent> turnStartedEvents = firedEvents.stream()
                    .filter(e -> e instanceof TurnStartedEvent)
                    .map(e -> (TurnStartedEvent) e)
                    .toList();

            assertThat(turnStartedEvents).hasSize(1);
        }

        @Test
        void turnStartedEventHasCorrectTurnNumber() {
            // Game ends after first turn
            when(gameState.isGameOver()).thenReturn(false, true);

            createTracker().run();

            TurnStartedEvent event = firedEvents.stream()
                    .filter(e -> e instanceof TurnStartedEvent)
                    .map(e -> (TurnStartedEvent) e)
                    .findFirst()
                    .orElseThrow();

            assertThat(event.turnNumber()).isEqualTo(1);
        }

        @Test
        void turnStartedEventHasCorrectActivePlayer() {
            // Game ends after first turn
            when(gameState.isGameOver()).thenReturn(false, true);

            createTracker().run();

            TurnStartedEvent event = firedEvents.stream()
                    .filter(e -> e instanceof TurnStartedEvent)
                    .map(e -> (TurnStartedEvent) e)
                    .findFirst()
                    .orElseThrow();

            assertThat(event.activePlayer()).isEqualTo(player2); // First player after initialization
        }
    }

    @Nested
    class TurnEndedEventFiring {

        @Test
        void firesTurnEndedEventAtEndOfTurn() {
            // Game ends after first turn
            when(gameState.isGameOver()).thenReturn(false, true);

            createTracker().run();

            List<TurnEndedEvent> turnEndedEvents = firedEvents.stream()
                    .filter(e -> e instanceof TurnEndedEvent)
                    .map(e -> (TurnEndedEvent) e)
                    .toList();

            assertThat(turnEndedEvents).hasSize(1);
        }

        @Test
        void turnEndedEventHasCorrectTurnNumber() {
            // Game ends after first turn
            when(gameState.isGameOver()).thenReturn(false, true);

            createTracker().run();

            TurnEndedEvent event = firedEvents.stream()
                    .filter(e -> e instanceof TurnEndedEvent)
                    .map(e -> (TurnEndedEvent) e)
                    .findFirst()
                    .orElseThrow();

            assertThat(event.turnNumber()).isEqualTo(1);
        }

        @Test
        void turnEndedEventFollowsTurnStarted() {
            // Game ends after first turn
            when(gameState.isGameOver()).thenReturn(false, true);

            createTracker().run();

            int startedIndex = -1;
            int endedIndex = -1;
            for (int i = 0; i < firedEvents.size(); i++) {
                if (firedEvents.get(i) instanceof TurnStartedEvent) {
                    startedIndex = i;
                } else if (firedEvents.get(i) instanceof TurnEndedEvent) {
                    endedIndex = i;
                }
            }

            assertThat(endedIndex).isGreaterThan(startedIndex);
        }
    }

    @Nested
    class MultipleTurns {

        @Test
        void firesEventsForEachTurn() {
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

            createTracker().run();

            List<TurnStartedEvent> turnStartedEvents = firedEvents.stream()
                    .filter(e -> e instanceof TurnStartedEvent)
                    .map(e -> (TurnStartedEvent) e)
                    .toList();

            assertThat(turnStartedEvents).hasSize(2);
            assertThat(turnStartedEvents.get(0).turnNumber()).isEqualTo(1);
            assertThat(turnStartedEvents.get(1).turnNumber()).isEqualTo(2);
        }

        @Test
        void activePlayerAlternatesCorrectly() {
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

            createTracker().run();

            List<Player> activePlayers = firedEvents.stream()
                    .filter(e -> e instanceof TurnStartedEvent)
                    .map(e -> ((TurnStartedEvent) e).activePlayer())
                    .toList();

            assertThat(activePlayers).containsExactly(player2, player1);
        }
    }
}
