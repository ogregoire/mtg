package be.imgn.mtg.engine.turn;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class PhaseTypeTest {

    @Nested
    class AllPhaseTypesDefined {

        @Test
        void hasAllExpectedPhases() {
            assertThat(PhaseType.values())
                    .containsExactly(PhaseType.BEGINNING, PhaseType.MAIN, PhaseType.COMBAT, PhaseType.ENDING);
        }

        @Test
        void fourPhaseTypes() {
            assertThat(PhaseType.values()).hasSize(4);
        }
    }

    @Nested
    class PhaseSteps {

        @Test
        void beginningPhaseHasThreeSteps() {
            assertThat(PhaseType.BEGINNING.steps()).containsExactly(StepType.UNTAP, StepType.UPKEEP, StepType.DRAW);
        }

        @Test
        void mainPhaseHasNoSteps() {
            assertThat(PhaseType.MAIN.steps()).isEmpty();
            assertThat(PhaseType.MAIN.hasSteps()).isFalse();
        }

        @Test
        void combatPhaseHasFiveSteps() {
            assertThat(PhaseType.COMBAT.steps())
                    .containsExactly(
                            StepType.BEGINNING_OF_COMBAT,
                            StepType.DECLARE_ATTACKERS,
                            StepType.DECLARE_BLOCKERS,
                            StepType.COMBAT_DAMAGE,
                            StepType.END_OF_COMBAT);
        }

        @Test
        void endingPhaseHasTwoSteps() {
            assertThat(PhaseType.ENDING.steps()).containsExactly(StepType.END, StepType.CLEANUP);
        }

        @Test
        void phasesWithStepsReportHasSteps() {
            assertThat(PhaseType.BEGINNING.hasSteps()).isTrue();
            assertThat(PhaseType.COMBAT.hasSteps()).isTrue();
            assertThat(PhaseType.ENDING.hasSteps()).isTrue();
        }
    }
}
