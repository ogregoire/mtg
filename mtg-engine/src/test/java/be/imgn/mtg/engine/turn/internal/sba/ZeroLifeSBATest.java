package be.imgn.mtg.engine.turn.internal.sba;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.state.GameState;
import be.imgn.mtg.engine.turn.StateBasedAction;

class ZeroLifeSBATest {

    private ZeroLifeSBA sba;
    private GameState gameState;

    @BeforeEach
    void setUp() {
        sba = new ZeroLifeSBA();
        gameState = mock(GameState.class);
    }

    @Nested
    class ImplementsStateBasedAction {

        @Test
        void implementsInterface() {
            assertThat(sba).isInstanceOf(StateBasedAction.class);
        }
    }

    @Nested
    class AppliesTo {

        @Test
        void returnsFalseInStubImplementation() {
            // Stub always returns false until player life tracking is implemented
            assertThat(sba.appliesTo(gameState)).isFalse();
        }
    }

    @Nested
    class Apply {

        @Test
        void canBeCalledWithoutException() {
            // Stub implementation - verify it doesn't throw
            sba.apply(gameState);
        }
    }
}
