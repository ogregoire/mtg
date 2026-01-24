package be.imgn.mtg.engine.turn.internal.steps;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.state.GameState;
import be.imgn.mtg.engine.turn.StepType;
import be.imgn.mtg.engine.turn.internal.Step;

class CombatStepsTest {

    private GameState gameState;

    @BeforeEach
    void setUp() {
        gameState = mock(GameState.class);
    }

    @Nested
    class BeginningOfCombatStepTests {

        private BeginningOfCombatStep step;

        @BeforeEach
        void setUp() {
            step = new BeginningOfCombatStep(1);
        }

        @Test
        void implementsInterface() {
            assertThat(step).isInstanceOf(Step.class);
        }

        @Test
        void returnsCorrectType() {
            assertThat(step.type()).isEqualTo(StepType.BEGINNING_OF_COMBAT);
        }

        @Test
        void tracksOccurrence() {
            assertThat(step.occurrence()).isEqualTo(1);
        }

        @Test
        void hasPriorityReturnsTrue() {
            // Players receive priority during beginning of combat (Rule 507.2)
            assertThat(step.hasPriority()).isTrue();
        }

        @Test
        void performTurnBasedActionsDoesNotThrow() {
            step.performTurnBasedActions(gameState);
        }
    }

    @Nested
    class DeclareAttackersStepTests {

        private DeclareAttackersStep step;

        @BeforeEach
        void setUp() {
            step = new DeclareAttackersStep(1);
        }

        @Test
        void implementsInterface() {
            assertThat(step).isInstanceOf(Step.class);
        }

        @Test
        void returnsCorrectType() {
            assertThat(step.type()).isEqualTo(StepType.DECLARE_ATTACKERS);
        }

        @Test
        void tracksOccurrence() {
            assertThat(step.occurrence()).isEqualTo(1);
        }

        @Test
        void hasPriorityReturnsTrue() {
            // Players receive priority during declare attackers (Rule 508.8)
            assertThat(step.hasPriority()).isTrue();
        }

        @Test
        void performTurnBasedActionsDoesNotThrow() {
            step.performTurnBasedActions(gameState);
        }
    }

    @Nested
    class DeclareBlockersStepTests {

        private DeclareBlockersStep step;

        @BeforeEach
        void setUp() {
            step = new DeclareBlockersStep(1);
        }

        @Test
        void implementsInterface() {
            assertThat(step).isInstanceOf(Step.class);
        }

        @Test
        void returnsCorrectType() {
            assertThat(step.type()).isEqualTo(StepType.DECLARE_BLOCKERS);
        }

        @Test
        void tracksOccurrence() {
            assertThat(step.occurrence()).isEqualTo(1);
        }

        @Test
        void hasPriorityReturnsTrue() {
            // Players receive priority during declare blockers (Rule 509.7)
            assertThat(step.hasPriority()).isTrue();
        }

        @Test
        void performTurnBasedActionsDoesNotThrow() {
            step.performTurnBasedActions(gameState);
        }
    }

    @Nested
    class CombatDamageStepTests {

        private CombatDamageStep step;

        @BeforeEach
        void setUp() {
            step = new CombatDamageStep(1);
        }

        @Test
        void implementsInterface() {
            assertThat(step).isInstanceOf(Step.class);
        }

        @Test
        void returnsCorrectType() {
            assertThat(step.type()).isEqualTo(StepType.COMBAT_DAMAGE);
        }

        @Test
        void tracksOccurrence() {
            assertThat(step.occurrence()).isEqualTo(1);
        }

        @Test
        void hasPriorityReturnsTrue() {
            // Players receive priority during combat damage (Rule 510.5)
            assertThat(step.hasPriority()).isTrue();
        }

        @Test
        void performTurnBasedActionsDoesNotThrow() {
            step.performTurnBasedActions(gameState);
        }
    }

    @Nested
    class EndOfCombatStepTests {

        private EndOfCombatStep step;

        @BeforeEach
        void setUp() {
            step = new EndOfCombatStep(1);
        }

        @Test
        void implementsInterface() {
            assertThat(step).isInstanceOf(Step.class);
        }

        @Test
        void returnsCorrectType() {
            assertThat(step.type()).isEqualTo(StepType.END_OF_COMBAT);
        }

        @Test
        void tracksOccurrence() {
            assertThat(step.occurrence()).isEqualTo(1);
        }

        @Test
        void hasPriorityReturnsTrue() {
            // Players receive priority during end of combat (Rule 511.2)
            assertThat(step.hasPriority()).isTrue();
        }

        @Test
        void performTurnBasedActionsDoesNotThrow() {
            step.performTurnBasedActions(gameState);
        }
    }
}
