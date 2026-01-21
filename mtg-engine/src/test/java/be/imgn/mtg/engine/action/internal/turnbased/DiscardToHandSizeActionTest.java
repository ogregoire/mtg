package be.imgn.mtg.engine.action.internal.turnbased;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.action.TurnBasedTiming;
import be.imgn.mtg.engine.event.GameEventProcessor;
import be.imgn.mtg.engine.state.GameState;

@DisplayName("DiscardToHandSizeAction")
class DiscardToHandSizeActionTest {

    private DiscardToHandSizeAction action;
    private GameState gameState;
    private GameEventProcessor processor;

    @BeforeEach
    void setUp() {
        action = new DiscardToHandSizeAction();
        gameState = mock(GameState.class);
        processor = mock(GameEventProcessor.class);
    }

    @Nested
    @DisplayName("timing")
    class TimingTests {

        @Test
        void returnsCleanupDiscard() {
            assertThat(action.timing()).isEqualTo(TurnBasedTiming.CLEANUP_DISCARD);
        }
    }

    @Nested
    @DisplayName("execute")
    class ExecuteTests {

        @Test
        void doesNotThrowException() {
            // Stub implementation - just verify it doesn't throw
            action.execute(gameState, processor);
        }
    }
}
