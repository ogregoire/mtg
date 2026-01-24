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

import be.imgn.mtg.engine.action.TurnBasedActionRegistry;
import be.imgn.mtg.engine.event.Event;
import be.imgn.mtg.engine.event.EventBus;
import be.imgn.mtg.engine.event.GameEventProcessor;
import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.state.GameState;
import be.imgn.mtg.engine.turn.DurationTracker;
import be.imgn.mtg.engine.turn.PhaseEndedEvent;
import be.imgn.mtg.engine.turn.PhaseStartedEvent;
import be.imgn.mtg.engine.turn.PhaseType;
import be.imgn.mtg.engine.turn.PrioritySystem;
import be.imgn.mtg.engine.turn.TurnEndedEvent;
import be.imgn.mtg.engine.zone.Stack;

class PhaseEventFiringTest {

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
    class PhaseStartedEventFiring {

        @Test
        void firesPhaseStartedForAllPhases() {
            // Game ends after first turn
            when(gameState.isGameOver()).thenAnswer(inv -> turnCounter.get() >= 1);

            createTracker().run();

            List<PhaseStartedEvent> phaseStartedEvents = firedEvents.stream()
                    .filter(e -> e instanceof PhaseStartedEvent)
                    .map(e -> (PhaseStartedEvent) e)
                    .toList();

            // 5 phases: BEGINNING, MAIN(1), COMBAT, MAIN(2), ENDING
            assertThat(phaseStartedEvents).hasSize(5);
        }

        @Test
        void phaseStartedEventsHaveCorrectTypes() {
            // Game ends after first turn
            when(gameState.isGameOver()).thenAnswer(inv -> turnCounter.get() >= 1);

            createTracker().run();

            List<PhaseType> phaseTypes = firedEvents.stream()
                    .filter(e -> e instanceof PhaseStartedEvent)
                    .map(e -> ((PhaseStartedEvent) e).phase())
                    .toList();

            assertThat(phaseTypes)
                    .containsExactly(
                            PhaseType.BEGINNING, PhaseType.MAIN, PhaseType.COMBAT, PhaseType.MAIN, PhaseType.ENDING);
        }

        @Test
        void mainPhasesHaveCorrectOccurrences() {
            // Game ends after first turn
            when(gameState.isGameOver()).thenAnswer(inv -> turnCounter.get() >= 1);

            createTracker().run();

            List<PhaseStartedEvent> mainPhaseEvents = firedEvents.stream()
                    .filter(e -> e instanceof PhaseStartedEvent)
                    .map(e -> (PhaseStartedEvent) e)
                    .filter(e -> e.phase() == PhaseType.MAIN)
                    .toList();

            assertThat(mainPhaseEvents).hasSize(2);
            assertThat(mainPhaseEvents.get(0).occurrence()).isEqualTo(1); // Pre-combat main
            assertThat(mainPhaseEvents.get(1).occurrence()).isEqualTo(2); // Post-combat main
        }
    }

    @Nested
    class PhaseEndedEventFiring {

        @Test
        void firesPhaseEndedForAllPhases() {
            // Game ends after first turn
            when(gameState.isGameOver()).thenAnswer(inv -> turnCounter.get() >= 1);

            createTracker().run();

            List<PhaseEndedEvent> phaseEndedEvents = firedEvents.stream()
                    .filter(e -> e instanceof PhaseEndedEvent)
                    .map(e -> (PhaseEndedEvent) e)
                    .toList();

            // 5 phases: BEGINNING, MAIN(1), COMBAT, MAIN(2), ENDING
            assertThat(phaseEndedEvents).hasSize(5);
        }

        @Test
        void phaseEndedFollowsPhaseStarted() {
            // Game ends after first turn
            when(gameState.isGameOver()).thenAnswer(inv -> turnCounter.get() >= 1);

            createTracker().run();

            // For each phase type, verify ended comes after started
            for (PhaseType phaseType : PhaseType.values()) {
                int startedIndex = -1;
                int endedIndex = -1;
                for (int i = 0; i < firedEvents.size(); i++) {
                    if (firedEvents.get(i) instanceof PhaseStartedEvent started && started.phase() == phaseType) {
                        startedIndex = i;
                    } else if (firedEvents.get(i) instanceof PhaseEndedEvent ended && ended.phase() == phaseType) {
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
    class PhaseEventMatching {

        @Test
        void phaseStartedAndEndedHaveMatchingOccurrences() {
            // Game ends after first turn
            when(gameState.isGameOver()).thenAnswer(inv -> turnCounter.get() >= 1);

            createTracker().run();

            List<PhaseStartedEvent> starts = firedEvents.stream()
                    .filter(e -> e instanceof PhaseStartedEvent)
                    .map(e -> (PhaseStartedEvent) e)
                    .toList();

            List<PhaseEndedEvent> ends = firedEvents.stream()
                    .filter(e -> e instanceof PhaseEndedEvent)
                    .map(e -> (PhaseEndedEvent) e)
                    .toList();

            assertThat(starts.size()).isEqualTo(ends.size());

            for (int i = 0; i < starts.size(); i++) {
                assertThat(starts.get(i).phase()).isEqualTo(ends.get(i).phase());
                assertThat(starts.get(i).occurrence()).isEqualTo(ends.get(i).occurrence());
            }
        }
    }
}
