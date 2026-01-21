package be.imgn.mtg.engine.action.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.action.TurnBasedAction;
import be.imgn.mtg.engine.action.TurnBasedActionRegistry;
import be.imgn.mtg.engine.action.TurnBasedTiming;
import be.imgn.mtg.engine.event.GameEventProcessor;
import be.imgn.mtg.engine.state.GameState;

@DisplayName("DefaultTurnBasedActionRegistry")
class DefaultTurnBasedActionRegistryTest {

    private GameState gameState;
    private GameEventProcessor processor;

    @BeforeEach
    void setUp() {
        gameState = mock(GameState.class);
        processor = mock(GameEventProcessor.class);
    }

    @Nested
    @DisplayName("getActionsFor")
    class GetActionsForTests {

        @Test
        void returnsEmptyListWhenNoActionsRegistered() {
            TurnBasedActionRegistry registry = new DefaultTurnBasedActionRegistry(List.of());

            var actions = registry.getActionsFor(TurnBasedTiming.UNTAP_STEP_UNTAP);

            assertThat(actions).isEmpty();
        }

        @Test
        void returnsActionsForMatchingTiming() {
            var untapAction = mockAction(TurnBasedTiming.UNTAP_STEP_UNTAP);
            TurnBasedActionRegistry registry = new DefaultTurnBasedActionRegistry(List.of(untapAction));

            var actions = registry.getActionsFor(TurnBasedTiming.UNTAP_STEP_UNTAP);

            assertThat(actions).containsExactly(untapAction);
        }

        @Test
        void returnsEmptyListForNonMatchingTiming() {
            var untapAction = mockAction(TurnBasedTiming.UNTAP_STEP_UNTAP);
            TurnBasedActionRegistry registry = new DefaultTurnBasedActionRegistry(List.of(untapAction));

            var actions = registry.getActionsFor(TurnBasedTiming.DRAW_STEP_DRAW);

            assertThat(actions).isEmpty();
        }

        @Test
        void returnsMultipleActionsForSameTiming() {
            var action1 = mockAction(TurnBasedTiming.CLEANUP_REMOVE_DAMAGE);
            var action2 = mockAction(TurnBasedTiming.CLEANUP_REMOVE_DAMAGE);
            TurnBasedActionRegistry registry = new DefaultTurnBasedActionRegistry(List.of(action1, action2));

            var actions = registry.getActionsFor(TurnBasedTiming.CLEANUP_REMOVE_DAMAGE);

            assertThat(actions).containsExactly(action1, action2);
        }

        @Test
        void returnsImmutableCopy() {
            var action = mockAction(TurnBasedTiming.DRAW_STEP_DRAW);
            TurnBasedActionRegistry registry = new DefaultTurnBasedActionRegistry(List.of(action));

            var actions = registry.getActionsFor(TurnBasedTiming.DRAW_STEP_DRAW);

            assertThat(actions).isUnmodifiable();
        }
    }

    @Nested
    @DisplayName("executeAll")
    class ExecuteAllTests {

        @Test
        void executesNoActionsWhenNoneRegistered() {
            TurnBasedActionRegistry registry = new DefaultTurnBasedActionRegistry(List.of());

            registry.executeAll(TurnBasedTiming.UNTAP_STEP_UNTAP, gameState, processor);

            // No exception thrown, no interactions expected
            verifyNoInteractions(processor);
        }

        @Test
        void executesActionForMatchingTiming() {
            var action = mockAction(TurnBasedTiming.UNTAP_STEP_UNTAP);
            TurnBasedActionRegistry registry = new DefaultTurnBasedActionRegistry(List.of(action));

            registry.executeAll(TurnBasedTiming.UNTAP_STEP_UNTAP, gameState, processor);

            verify(action).execute(gameState, processor);
        }

        @Test
        void doesNotExecuteActionsForDifferentTiming() {
            var action = mockAction(TurnBasedTiming.UNTAP_STEP_UNTAP);
            TurnBasedActionRegistry registry = new DefaultTurnBasedActionRegistry(List.of(action));

            registry.executeAll(TurnBasedTiming.DRAW_STEP_DRAW, gameState, processor);

            verifyNoInteractions(gameState);
        }

        @Test
        void executesMultipleActionsInOrder() {
            var action1 = mockAction(TurnBasedTiming.CLEANUP_DISCARD);
            var action2 = mockAction(TurnBasedTiming.CLEANUP_DISCARD);
            TurnBasedActionRegistry registry = new DefaultTurnBasedActionRegistry(List.of(action1, action2));

            registry.executeAll(TurnBasedTiming.CLEANUP_DISCARD, gameState, processor);

            var ordered = inOrder(action1, action2);
            ordered.verify(action1).execute(gameState, processor);
            ordered.verify(action2).execute(gameState, processor);
        }
    }

    private TurnBasedAction mockAction(TurnBasedTiming timing) {
        var action = mock(TurnBasedAction.class);
        when(action.timing()).thenReturn(timing);
        return action;
    }
}
