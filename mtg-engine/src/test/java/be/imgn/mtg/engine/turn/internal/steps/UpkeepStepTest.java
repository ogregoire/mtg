package be.imgn.mtg.engine.turn.internal.steps;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.state.GameState;
import be.imgn.mtg.engine.turn.StepType;
import be.imgn.mtg.engine.turn.internal.Step;

class UpkeepStepTest {

    private UpkeepStep step;
    private GameState gameState;

    @BeforeEach
    void setUp() {
        step = new UpkeepStep(1);
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
        void returnsUpkeepType() {
            assertThat(step.type()).isEqualTo(StepType.UPKEEP);
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
            var secondOccurrence = new UpkeepStep(2);
            assertThat(secondOccurrence.occurrence()).isEqualTo(2);
        }
    }

    @Nested
    class HasPriority {

        @Test
        void returnsTrue() {
            // Players receive priority during the upkeep step (Rule 503.2)
            assertThat(step.hasPriority()).isTrue();
        }
    }

    @Nested
    class PerformTurnBasedActions {

        @Test
        void canBeCalledWithoutException() {
            // No turn-based actions in upkeep step
            step.performTurnBasedActions(gameState);
        }
    }
}
