package be.imgn.mtg.engine.turn.internal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
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
import be.imgn.mtg.engine.zone.Stack;

class CleanupLoopTest {

    private GameState gameState;
    private EventBus eventBus;
    private OccurrenceTracker occurrenceTracker;
    private PrioritySystem prioritySystem;
    private SBAEngine sbaEngine;
    private DurationTracker durationTracker;
    private SkipTracker skipTracker;
    private Stack stack;

    private Player player1;
    private List<Event> firedEvents;
    private AtomicInteger turnCounter;

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
        turnCounter = new AtomicInteger(0);

        when(gameState.players()).thenReturn(List.of(player1));
        when(gameState.nextPlayerInTurnOrder(player1)).thenReturn(player1);
        when(gameState.stack()).thenReturn(stack);
        when(stack.isEmpty()).thenReturn(true);
        when(prioritySystem.allPassed()).thenReturn(true);

        firedEvents = new ArrayList<>();
        doAnswer(inv -> {
                    firedEvents.add(inv.getArgument(0));
                    if (inv.getArgument(0) instanceof TurnEndedEvent) {
                        turnCounter.incrementAndGet();
                    }
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
    class SBAsDuringCleanup {

        @Test
        void cleanupChecksSBAsWouldApply() {
            // Game ends after first turn
            when(gameState.isGameOver()).thenAnswer(inv -> turnCounter.get() >= 1);

            // SBAs would not apply
            when(sbaEngine.wouldPerformActions(gameState)).thenReturn(false);

            var tracker = createTracker();
            tracker.run();

            // Should have checked wouldPerformActions during cleanup
            verify(sbaEngine, atLeast(1)).wouldPerformActions(gameState);
        }

        @Test
        void expiresUntilEndOfTurnDuringCleanup() {
            // Game ends after first turn
            when(gameState.isGameOver()).thenAnswer(inv -> turnCounter.get() >= 1);
            when(sbaEngine.wouldPerformActions(gameState)).thenReturn(false);

            var tracker = createTracker();
            tracker.run();

            // Should expire "until end of turn" effects
            verify(durationTracker, atLeast(1)).expireUntilEndOfTurn();
        }
    }
}
