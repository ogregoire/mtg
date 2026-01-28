package be.imgn.mtg.engine.turn.internal.sba;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.state.GameState;

class StateBasedActionsTest {

    GameState gameState = mock(GameState.class);

    @Nested
    class ZeroLifeSBATests {

        @Test
        void checkAndApplyReturnsFalseForStub() {
            var sba = new ZeroLifeSBA(gameState);

            assertThat(sba.checkAndApply()).isFalse();
        }
    }

    @Nested
    class LethalDamageSBATests {

        @Test
        void checkAndApplyReturnsFalseForStub() {
            var sba = new LethalDamageSBA(gameState);

            assertThat(sba.checkAndApply()).isFalse();
        }
    }

    @Nested
    class ZeroToughnessSBATests {

        @Test
        void checkAndApplyReturnsFalseForStub() {
            var sba = new ZeroToughnessSBA(gameState);

            assertThat(sba.checkAndApply()).isFalse();
        }
    }

    @Nested
    class ZeroLoyaltySBATests {

        @Test
        void checkAndApplyReturnsFalseForStub() {
            var sba = new ZeroLoyaltySBA(gameState);

            assertThat(sba.checkAndApply()).isFalse();
        }
    }

    @Nested
    class EmptyLibraryLossSBATests {

        @Test
        void checkAndApplyReturnsFalseForStub() {
            var sba = new EmptyLibraryLossSBA(gameState);

            assertThat(sba.checkAndApply()).isFalse();
        }
    }

    @Nested
    class PoisonCounterLossSBATests {

        @Test
        void checkAndApplyReturnsFalseForStub() {
            var sba = new PoisonCounterLossSBA(gameState);

            assertThat(sba.checkAndApply()).isFalse();
        }
    }

    @Nested
    class LegendRuleSBATests {

        @Test
        void checkAndApplyReturnsFalseForStub() {
            var sba = new LegendRuleSBA(gameState);

            assertThat(sba.checkAndApply()).isFalse();
        }
    }

    @Nested
    class WorldRuleSBATests {

        @Test
        void checkAndApplyReturnsFalseForStub() {
            var sba = new WorldRuleSBA(gameState);

            assertThat(sba.checkAndApply()).isFalse();
        }
    }

    @Nested
    class TokenCeasesToExistSBATests {

        @Test
        void checkAndApplyReturnsFalseForStub() {
            var sba = new TokenCeasesToExistSBA(gameState);

            assertThat(sba.checkAndApply()).isFalse();
        }
    }

    @Nested
    class CopyCeasesToExistSBATests {

        @Test
        void checkAndApplyReturnsFalseForStub() {
            var sba = new CopyCeasesToExistSBA(gameState);

            assertThat(sba.checkAndApply()).isFalse();
        }
    }

    @Nested
    class DeathtouchDamageSBATests {

        @Test
        void checkAndApplyReturnsFalseForStub() {
            var sba = new DeathtouchDamageSBA(gameState);

            assertThat(sba.checkAndApply()).isFalse();
        }
    }

    @Nested
    class IllegalAuraSBATests {

        @Test
        void checkAndApplyReturnsFalseForStub() {
            var sba = new IllegalAuraSBA(gameState);

            assertThat(sba.checkAndApply()).isFalse();
        }
    }

    @Nested
    class IllegalEquipmentSBATests {

        @Test
        void checkAndApplyReturnsFalseForStub() {
            var sba = new IllegalEquipmentSBA(gameState);

            assertThat(sba.checkAndApply()).isFalse();
        }
    }

    @Nested
    class IllegalAttachmentSBATests {

        @Test
        void checkAndApplyReturnsFalseForStub() {
            var sba = new IllegalAttachmentSBA(gameState);

            assertThat(sba.checkAndApply()).isFalse();
        }
    }

    @Nested
    class CounterCancellationSBATests {

        @Test
        void checkAndApplyReturnsFalseForStub() {
            var sba = new CounterCancellationSBA(gameState);

            assertThat(sba.checkAndApply()).isFalse();
        }
    }

    @Nested
    class CounterLimitSBATests {

        @Test
        void checkAndApplyReturnsFalseForStub() {
            var sba = new CounterLimitSBA(gameState);

            assertThat(sba.checkAndApply()).isFalse();
        }
    }

    @Nested
    class SagaFinalChapterSBATests {

        @Test
        void checkAndApplyReturnsFalseForStub() {
            var sba = new SagaFinalChapterSBA(gameState);

            assertThat(sba.checkAndApply()).isFalse();
        }
    }

    @Nested
    class DungeonCompletedSBATests {

        @Test
        void checkAndApplyReturnsFalseForStub() {
            var sba = new DungeonCompletedSBA(gameState);

            assertThat(sba.checkAndApply()).isFalse();
        }
    }

    @Nested
    class BattleNoProtectorSBATests {

        @Test
        void checkAndApplyReturnsFalseForStub() {
            var sba = new BattleNoProtectorSBA(gameState);

            assertThat(sba.checkAndApply()).isFalse();
        }
    }

    @Nested
    class BattleZeroDefenseSBATests {

        @Test
        void checkAndApplyReturnsFalseForStub() {
            var sba = new BattleZeroDefenseSBA(gameState);

            assertThat(sba.checkAndApply()).isFalse();
        }
    }

    @Nested
    class SiegeProtectorSBATests {

        @Test
        void checkAndApplyReturnsFalseForStub() {
            var sba = new SiegeProtectorSBA(gameState);

            assertThat(sba.checkAndApply()).isFalse();
        }
    }

    @Nested
    class MultipleRolesSBATests {

        @Test
        void checkAndApplyReturnsFalseForStub() {
            var sba = new MultipleRolesSBA(gameState);

            assertThat(sba.checkAndApply()).isFalse();
        }
    }
}
