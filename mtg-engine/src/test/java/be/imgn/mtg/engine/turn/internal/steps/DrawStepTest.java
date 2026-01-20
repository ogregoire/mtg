package be.imgn.mtg.engine.turn.internal.steps;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.state.GameState;
import be.imgn.mtg.engine.turn.Step;
import be.imgn.mtg.engine.turn.StepType;

class DrawStepTest {

    private DrawStep step;
    private GameState gameState;

    @BeforeEach
    void setUp() {
        step = new DrawStep(1);
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
        void returnsDrawType() {
            assertThat(step.type()).isEqualTo(StepType.DRAW);
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
            var secondOccurrence = new DrawStep(2);
            assertThat(secondOccurrence.occurrence()).isEqualTo(2);
        }
    }

    @Nested
    class HasPriority {

        @Test
        void returnsTrue() {
            // Players receive priority during the draw step (Rule 504.2)
            assertThat(step.hasPriority()).isTrue();
        }
    }

    @Nested
    class PerformTurnBasedActions {

        @Test
        void canBeCalledWithoutException() {
            // Currently a stub - verify it doesn't throw
            // Full implementation would draw a card for active player
            step.performTurnBasedActions(gameState);
        }
    }
}
