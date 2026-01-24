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
import be.imgn.mtg.engine.turn.PrioritySystem;
import be.imgn.mtg.engine.turn.StepEndedEvent;
import be.imgn.mtg.engine.turn.StepStartedEvent;
import be.imgn.mtg.engine.turn.StepType;
import be.imgn.mtg.engine.turn.TurnEndedEvent;
import be.imgn.mtg.engine.zone.Stack;

class StepEventFiringTest {

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
    class StepStartedEventFiring {

        @Test
        void firesStepStartedForAllSteps() {
            // Game ends after first turn
            when(gameState.isGameOver()).thenAnswer(inv -> turnCounter.get() >= 1);

            createTracker().run();

            List<StepStartedEvent> stepStartedEvents = firedEvents.stream()
                    .gather(instanceOf(StepStartedEvent.class))
                    .toList();

            // 10 steps: 3 beginning, 5 combat, 2 ending
            // Note: Main phase "steps" (pseudo-steps) don't fire step events
            assertThat(stepStartedEvents).hasSize(10);
        }

        @Test
        void stepEventsInCorrectOrder() {
            // Game ends after first turn
            when(gameState.isGameOver()).thenAnswer(inv -> turnCounter.get() >= 1);

            createTracker().run();

            List<StepType> stepTypes = firedEvents.stream()
                    .gather(instanceOf(StepStartedEvent.class))
                    .map(StepStartedEvent::step)
                    .toList();

            assertThat(stepTypes)
                    .containsExactly(
                            StepType.UNTAP,
                            StepType.UPKEEP,
                            StepType.DRAW,
                            StepType.BEGINNING_OF_COMBAT,
                            StepType.DECLARE_ATTACKERS,
                            StepType.DECLARE_BLOCKERS,
                            StepType.COMBAT_DAMAGE,
                            StepType.END_OF_COMBAT,
                            StepType.END,
                            StepType.CLEANUP);
        }

        @Test
        void stepEventsHaveOccurrenceOne() {
            // Game ends after first turn
            when(gameState.isGameOver()).thenAnswer(inv -> turnCounter.get() >= 1);

            createTracker().run();

            List<StepStartedEvent> stepStartedEvents = firedEvents.stream()
                    .gather(instanceOf(StepStartedEvent.class))
                    .toList();

            for (StepStartedEvent event : stepStartedEvents) {
                assertThat(event.occurrence()).isEqualTo(1);
            }
        }
    }

    @Nested
    class StepEndedEventFiring {

        @Test
        void firesStepEndedForAllSteps() {
            // Game ends after first turn
            when(gameState.isGameOver()).thenAnswer(inv -> turnCounter.get() >= 1);

            createTracker().run();

            List<StepEndedEvent> stepEndedEvents = firedEvents.stream()
                    .gather(instanceOf(StepEndedEvent.class))
                    .toList();

            // 10 steps: 3 beginning, 5 combat, 2 ending
            assertThat(stepEndedEvents).hasSize(10);
        }

        @Test
        void stepEndedFollowsStepStarted() {
            // Game ends after first turn
            when(gameState.isGameOver()).thenAnswer(inv -> turnCounter.get() >= 1);

            createTracker().run();

            // For each step type, verify ended comes after started
            for (StepType stepType : StepType.values()) {
                int startedIndex = -1;
                int endedIndex = -1;
                for (int i = 0; i < firedEvents.size(); i++) {
                    if (firedEvents.get(i) instanceof StepStartedEvent started && started.step() == stepType) {
                        startedIndex = i;
                    } else if (firedEvents.get(i) instanceof StepEndedEvent ended && ended.step() == stepType) {
                        endedIndex = i;
                        break; // First match
                    }
                }
                if (startedIndex >= 0 && endedIndex >= 0) {
                    assertThat(endedIndex).isGreaterThan(startedIndex);
                }
            }
        }
    }

    @Nested
    class StepEventMatching {

        @Test
        void stepStartedAndEndedHaveMatchingValues() {
            // Game ends after first turn
            when(gameState.isGameOver()).thenAnswer(inv -> turnCounter.get() >= 1);

            createTracker().run();

            List<StepStartedEvent> starts = firedEvents.stream()
                    .gather(instanceOf(StepStartedEvent.class))
                    .toList();

            List<StepEndedEvent> ends = firedEvents.stream()
                    .gather(instanceOf(StepEndedEvent.class))
                    .toList();

            assertThat(starts.size()).isEqualTo(ends.size());

            for (int i = 0; i < starts.size(); i++) {
                assertThat(starts.get(i).step()).isEqualTo(ends.get(i).step());
                assertThat(starts.get(i).occurrence()).isEqualTo(ends.get(i).occurrence());
            }
        }
    }
}
