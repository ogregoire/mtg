package be.imgn.mtg.engine.turn.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
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
import be.imgn.mtg.engine.turn.PrioritySystem;
import be.imgn.mtg.engine.turn.TurnEndedEvent;
import be.imgn.mtg.engine.zone.Stack;

class PriorityRoundTest {

    private GameState gameState;
    private EventBus eventBus;
    private PrioritySystem prioritySystem;
    private DurationTracker durationTracker;
    private Stack stack;
    private TurnBasedActionRegistry turnBasedActionRegistry;
    private GameEventProcessor gameEventProcessor;
    private StateBasedAction mockSba;

    private Player player1;
    private Player player2;
    private List<Event> firedEvents;
    private AtomicInteger turnCounter;

    @BeforeEach
    void setUp() {
        gameState = mock(GameState.class);
        eventBus = mock(EventBus.class);
        prioritySystem = new DefaultPrioritySystem(gameState);
        durationTracker = mock(DurationTracker.class);
        stack = mock(Stack.class);
        turnBasedActionRegistry = mock(TurnBasedActionRegistry.class);
        gameEventProcessor = mock(GameEventProcessor.class);
        mockSba = mock(StateBasedAction.class);

        player1 = mock(Player.class);
        player2 = mock(Player.class);
        turnCounter = new AtomicInteger(0);

        when(gameState.players()).thenReturn(List.of(player1, player2));
        when(gameState.activePlayer()).thenReturn(player1);
        when(gameState.nextPlayerInTurnOrder(player1)).thenReturn(player2);
        when(gameState.nextPlayerInTurnOrder(player2)).thenReturn(player1);
        when(gameState.stack()).thenReturn(stack);
        when(stack.isEmpty()).thenReturn(true);

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
                List.of(mockSba),
                durationTracker,
                turnBasedActionRegistry,
                gameEventProcessor);
    }

    @Nested
    class SBAStabilization {

        @Test
        void checksSBAsBeforePassingPriority() {
            // Game ends after first turn
            when(gameState.isGameOver()).thenAnswer(inv -> turnCounter.get() >= 1);
            when(mockSba.appliesTo(gameState)).thenReturn(false);

            var tracker = createTracker();
            tracker.run();

            // SBAs should have been checked multiple times during priority rounds
            verify(mockSba, atLeastOnce()).appliesTo(gameState);
        }
    }

    @Nested
    class AllPassedExitCondition {

        @Test
        void priorityRoundEndsWhenAllPassAndStackEmpty() {
            // Game ends after first turn
            when(gameState.isGameOver()).thenAnswer(inv -> turnCounter.get() >= 1);
            when(mockSba.appliesTo(gameState)).thenReturn(false);

            var tracker = createTracker();
            tracker.run();

            // Turn should have completed (all priority rounds ended)
            assertThat(tracker.turnState().turnNumber()).isEqualTo(1);
        }
    }
}
