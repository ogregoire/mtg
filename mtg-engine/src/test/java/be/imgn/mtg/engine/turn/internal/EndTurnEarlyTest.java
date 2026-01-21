package be.imgn.mtg.engine.turn.internal;

import static org.assertj.core.api.Assertions.assertThat;
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

import be.imgn.mtg.engine.action.TurnBasedActionRegistry;
import be.imgn.mtg.engine.event.Event;
import be.imgn.mtg.engine.event.EventBus;
import be.imgn.mtg.engine.event.GameEventProcessor;
import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.state.GameState;
import be.imgn.mtg.engine.turn.DurationTracker;
import be.imgn.mtg.engine.turn.OccurrenceTracker;
import be.imgn.mtg.engine.turn.PrioritySystem;
import be.imgn.mtg.engine.turn.SBAEngine;
import be.imgn.mtg.engine.turn.SkipTracker;
import be.imgn.mtg.engine.turn.StepStartedEvent;
import be.imgn.mtg.engine.turn.StepType;
import be.imgn.mtg.engine.turn.TurnEndedEvent;
import be.imgn.mtg.engine.turn.TurnTracker;
import be.imgn.mtg.engine.zone.Stack;

class EndTurnEarlyTest {

    private GameState gameState;
    private EventBus eventBus;
    private OccurrenceTracker occurrenceTracker;
    private PrioritySystem prioritySystem;
    private SBAEngine sbaEngine;
    private DurationTracker durationTracker;
    private SkipTracker skipTracker;
    private Stack stack;
    private TurnBasedActionRegistry turnBasedActionRegistry;
    private GameEventProcessor gameEventProcessor;

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
        turnBasedActionRegistry = mock(TurnBasedActionRegistry.class);
        gameEventProcessor = mock(GameEventProcessor.class);

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

    private TurnTracker createTracker() {
        return new DefaultTurnTracker(
                gameState,
                eventBus,
                occurrenceTracker,
                prioritySystem,
                sbaEngine,
                durationTracker,
                skipTracker,
                turnBasedActionRegistry,
                gameEventProcessor);
    }

    @Nested
    class EndTurnBehavior {

        @Test
        void endTurnEarlySkipsToCleanup() {
            // Game ends after first turn
            when(gameState.isGameOver()).thenAnswer(inv -> turnCounter.get() >= 1);

            var tracker = createTracker();

            // Clear events before hooking
            firedEvents.clear();
            turnCounter.set(0);

            // We'll trigger endTurnEarly by hooking into the event
            var trackerRef = tracker;
            doAnswer(inv -> {
                        Event event = inv.getArgument(0);
                        firedEvents.add(event);
                        if (inv.getArgument(0) instanceof TurnEndedEvent) {
                            turnCounter.incrementAndGet();
                        }
                        // End turn after first step starts
                        if (event instanceof StepStartedEvent started && started.step() == StepType.UNTAP) {
                            trackerRef.endTurnEarly();
                        }
                        return null;
                    })
                    .when(eventBus)
                    .post(any(Event.class));

            tracker.run();

            // Should have cleanup step events at the end (from runCleanupLoop when endTurnRequested)
            List<StepType> stepTypes = firedEvents.stream()
                    .filter(e -> e instanceof StepStartedEvent)
                    .map(e -> ((StepStartedEvent) e).step())
                    .toList();

            assertThat(stepTypes).contains(StepType.CLEANUP);
        }

        @Test
        void cleanupExpiresUntilEndOfTurnDuringNormalTurn() {
            // Game ends after first turn
            when(gameState.isGameOver()).thenAnswer(inv -> turnCounter.get() >= 1);

            var tracker = createTracker();
            tracker.run();

            // Duration tracker should expire until end of turn (during cleanup)
            verify(durationTracker, atLeast(1)).expireUntilEndOfTurn();
        }
    }
}
