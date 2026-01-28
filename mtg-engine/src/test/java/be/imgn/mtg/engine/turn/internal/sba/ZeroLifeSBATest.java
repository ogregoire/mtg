package be.imgn.mtg.engine.turn.internal.sba;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.state.GameState;
import be.imgn.mtg.engine.turn.internal.StateBasedAction;

class ZeroLifeSBATest {

    private ZeroLifeSBA sba;

    @BeforeEach
    void setUp() {
        sba = new ZeroLifeSBA(mock(GameState.class));
    }

    @Nested
    class ImplementsStateBasedAction {

        @Test
        void implementsInterface() {
            assertThat(sba).isInstanceOf(StateBasedAction.class);
        }
    }

    @Nested
    class CheckAndApply {

        @Test
        void returnsFalseInStubImplementation() {
            assertThat(sba.checkAndApply()).isFalse();
        }
    }
}
