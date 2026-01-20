package be.imgn.mtg.engine.turn;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class StepTypeTest {

    @Nested
    class AllStepTypesDefined {

        @Test
        void hasAllExpectedSteps() {
            assertThat(StepType.values())
                    .containsExactly(
                            StepType.UNTAP,
                            StepType.UPKEEP,
                            StepType.DRAW,
                            StepType.BEGINNING_OF_COMBAT,
                            StepType.DECLARE_ATTACKERS,
                            StepType.DECLARE_BLOCKERS,
                            StepType.COMBAT_DAMAGE,
                            StepType.END_OF_COMBAT,
                            StepType.END,
                            StepType.CLEANUP);
        }

        @Test
        void tenStepTypes() {
            assertThat(StepType.values()).hasSize(10);
        }
    }

    @Nested
    class PriorityBehavior {

        @Test
        void untapStepHasNoPriority() {
            assertThat(StepType.UNTAP.hasPriority()).isFalse();
        }

        @Test
        void cleanupStepHasNoPriorityNormally() {
            assertThat(StepType.CLEANUP.hasPriority()).isFalse();
        }

        @Test
        void upkeepStepHasPriority() {
            assertThat(StepType.UPKEEP.hasPriority()).isTrue();
        }

        @Test
        void drawStepHasPriority() {
            assertThat(StepType.DRAW.hasPriority()).isTrue();
        }

        @Test
        void allCombatStepsHavePriority() {
            assertThat(StepType.BEGINNING_OF_COMBAT.hasPriority()).isTrue();
            assertThat(StepType.DECLARE_ATTACKERS.hasPriority()).isTrue();
            assertThat(StepType.DECLARE_BLOCKERS.hasPriority()).isTrue();
            assertThat(StepType.COMBAT_DAMAGE.hasPriority()).isTrue();
            assertThat(StepType.END_OF_COMBAT.hasPriority()).isTrue();
        }

        @Test
        void endStepHasPriority() {
            assertThat(StepType.END.hasPriority()).isTrue();
        }
    }
}
