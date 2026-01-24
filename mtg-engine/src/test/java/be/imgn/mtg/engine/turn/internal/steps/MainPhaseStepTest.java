package be.imgn.mtg.engine.turn.internal.steps;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.state.GameState;
import be.imgn.mtg.engine.turn.internal.Step;

class MainPhaseStepTest {

    private MainPhaseStep step;
    private GameState gameState;

    @BeforeEach
    void setUp() {
        step = new MainPhaseStep(1);
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
        void returnsNullSinceMainPhaseIsNotAStep() {
            // Main phase doesn't have a StepType - it's a pseudo-step
            assertThat(step.type()).isNull();
        }
    }

    @Nested
    class Occurrence {

        @Test
        void returnsOccurrenceFromConstructor() {
            assertThat(step.occurrence()).isEqualTo(1);
        }

        @Test
        void distinguishesPreAndPostCombatMainPhase() {
            var firstMain = new MainPhaseStep(1);
            var secondMain = new MainPhaseStep(2);

            assertThat(firstMain.occurrence()).isEqualTo(1);
            assertThat(secondMain.occurrence()).isEqualTo(2);
        }
    }

    @Nested
    class HasPriority {

        @Test
        void returnsTrue() {
            // Players receive priority during the main phase (Rule 505.2)
            assertThat(step.hasPriority()).isTrue();
        }
    }

    @Nested
    class PerformTurnBasedActions {

        @Test
        void canBeCalledWithoutException() {
            // Currently a stub - verify it doesn't throw
            // Full implementation would place saga counters (Rule 714.3)
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
