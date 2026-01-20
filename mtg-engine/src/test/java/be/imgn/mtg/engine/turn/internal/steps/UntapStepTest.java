package be.imgn.mtg.engine.turn.internal.steps;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.state.GameState;
import be.imgn.mtg.engine.turn.Step;
import be.imgn.mtg.engine.turn.StepType;

class UntapStepTest {

    private UntapStep step;
    private GameState gameState;

    @BeforeEach
    void setUp() {
        step = new UntapStep(1);
        gameState = mock(GameState.class);
    }

    @Nested
    class ImplementsStep {

        @Test
        void implementsInterface() {
            assertThat(step).isInstanceOf(Step.class);
        }
    }

    @Nested
    class Type {

        @Test
        void returnsUntapType() {
            assertThat(step.type()).isEqualTo(StepType.UNTAP);
        }
    }

    @Nested
    class Occurrence {

        @Test
        void returnsOccurrenceFromConstructor() {
            assertThat(step.occurrence()).isEqualTo(1);
        }

        @Test
        void tracksMultipleOccurrences() {
            var secondOccurrence = new UntapStep(2);
            assertThat(secondOccurrence.occurrence()).isEqualTo(2);
        }
    }

    @Nested
    class HasPriority {

        @Test
        void returnsFalse() {
            // No player receives priority during the untap step (Rule 502.4)
            assertThat(step.hasPriority()).isFalse();
        }
    }

    @Nested
    class PerformTurnBasedActions {

        @Test
        void canBeCalledWithoutException() {
            // Currently a stub - verify it doesn't throw
            step.performTurnBasedActions(gameState);
        }
    }

    @Nested
    class PerformEndActions {

        @Test
        void canBeCalledWithoutException() {
            step.performEndActions(gameState);
        }
    }
}
