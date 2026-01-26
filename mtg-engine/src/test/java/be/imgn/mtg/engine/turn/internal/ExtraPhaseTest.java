package be.imgn.mtg.engine.turn.internal;

import static be.imgn.mtg.engine.util.MoreGatherers.instanceOf;
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

import be.imgn.mtg.engine.action.TurnBasedActionRegistry;
import be.imgn.mtg.engine.event.Event;
import be.imgn.mtg.engine.event.EventBus;
import be.imgn.mtg.engine.event.GameEventProcessor;
import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.state.GameState;
import be.imgn.mtg.engine.turn.DurationTracker;
import be.imgn.mtg.engine.turn.PhaseStartedEvent;
import be.imgn.mtg.engine.turn.PhaseType;
import be.imgn.mtg.engine.turn.PrioritySystem;
import be.imgn.mtg.engine.turn.TurnEndedEvent;
import be.imgn.mtg.engine.zone.Stack;

class ExtraPhaseTest {

    private GameState gameState;
    private EventBus eventBus;
    private PrioritySystem prioritySystem;
    private DurationTracker durationTracker;
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
        prioritySystem = mock(PrioritySystem.class);
        durationTracker = mock(DurationTracker.class);
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

    private DefaultTurnTracker createTracker() {
        return new DefaultTurnTracker(
                gameState,
                eventBus,
                prioritySystem,
                List.of(), // empty SBA list for tests
                durationTracker,
                turnBasedActionRegistry,
                gameEventProcessor);
    }

    @Nested
    class PhaseOccurrenceTracking {

        @Test
        void mainPhasesHaveDifferentOccurrences() {
            // Game ends after first turn
            when(gameState.isGameOver()).thenAnswer(inv -> turnCounter.get() >= 1);

            createTracker().run();

            // Verify two main phases fired with different occurrences
            var mainPhaseEvents = firedEvents.stream()
                    .gather(instanceOf(PhaseStartedEvent.class))
                    .filter(e -> e.phase() == PhaseType.MAIN)
                    .toList();

            assertThat(mainPhaseEvents).hasSize(2);
            assertThat(mainPhaseEvents.get(0).occurrence()).isEqualTo(1);
            assertThat(mainPhaseEvents.get(1).occurrence()).isEqualTo(2);
        }

        @Test
        void allPhasesFireInCorrectOrder() {
            // Game ends after first turn
            when(gameState.isGameOver()).thenAnswer(inv -> turnCounter.get() >= 1);

            createTracker().run();

            var phaseOrder = firedEvents.stream()
                    .gather(instanceOf(PhaseStartedEvent.class))
                    .map(PhaseStartedEvent::phase)
                    .toList();

            assertThat(phaseOrder)
                    .containsExactly(
                            PhaseType.BEGINNING,
                            PhaseType.MAIN, // First main
                            PhaseType.COMBAT,
                            PhaseType.MAIN, // Second main
                            PhaseType.ENDING);
        }
    }
}
