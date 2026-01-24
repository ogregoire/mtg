package be.imgn.mtg.engine.turn.internal.steps;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.state.GameState;
import be.imgn.mtg.engine.turn.StepType;
import be.imgn.mtg.engine.turn.internal.Step;

class CleanupStepTest {

    private CleanupStep step;
    private GameState gameState;

    @BeforeEach
    void setUp() {
        step = new CleanupStep(1);
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
        void returnsCleanupType() {
            assertThat(step.type()).isEqualTo(StepType.CLEANUP);
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
            var secondOccurrence = new CleanupStep(2);
            assertThat(secondOccurrence.occurrence()).isEqualTo(2);
        }
    }

    @Nested
    class HasPriority {

        @Test
        void returnsFalseNormally() {
            // Normally no player receives priority during cleanup (Rule 514.3)
            assertThat(step.hasPriority()).isFalse();
        }
    }

    @Nested
    class PerformTurnBasedActions {

        @Test
        void canBeCalledWithoutException() {
            // Currently a stub - verify it doesn't throw
            // Full implementation would:
            // - Discard to maximum hand size (Rule 514.1)
            // - Remove damage from permanents (Rule 514.2)
            step.performTurnBasedActions(gameState);
        }
    }

    @Nested
    class CleanupLoop {

        @Test
        void shouldRepeatReturnsFalseInitially() {
            assertThat(step.shouldRepeat()).isFalse();
        }

        @Test
        void shouldRepeatReturnsTrueAfterMarkTriggeredLoop() {
            step.markTriggeredLoop();
            assertThat(step.shouldRepeat()).isTrue();
        }

        @Test
        void markTriggeredLoopCanBeCalledMultipleTimes() {
            step.markTriggeredLoop();
            step.markTriggeredLoop();
            assertThat(step.shouldRepeat()).isTrue();
        }
    }
}
