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
        void appliesToReturnsFalseForStub() {
            var sba = new ZeroLifeSBA();

            assertThat(sba.appliesTo(gameState)).isFalse();
        }

        @Test
        void applyDoesNothingForStub() {
            var sba = new ZeroLifeSBA();

            // Should not throw
            sba.apply(gameState);
        }
    }

    @Nested
    class LethalDamageSBATests {

        @Test
        void appliesToReturnsFalseForStub() {
            var sba = new LethalDamageSBA();

            assertThat(sba.appliesTo(gameState)).isFalse();
        }

        @Test
        void applyDoesNothingForStub() {
            var sba = new LethalDamageSBA();

            sba.apply(gameState);
        }
    }

    @Nested
    class ZeroToughnessSBATests {

        @Test
        void appliesToReturnsFalseForStub() {
            var sba = new ZeroToughnessSBA();

            assertThat(sba.appliesTo(gameState)).isFalse();
        }

        @Test
        void applyDoesNothingForStub() {
            var sba = new ZeroToughnessSBA();

            sba.apply(gameState);
        }
    }

    @Nested
    class ZeroLoyaltySBATests {

        @Test
        void appliesToReturnsFalseForStub() {
            var sba = new ZeroLoyaltySBA();

            assertThat(sba.appliesTo(gameState)).isFalse();
        }

        @Test
        void applyDoesNothingForStub() {
            var sba = new ZeroLoyaltySBA();

            sba.apply(gameState);
        }
    }

    @Nested
    class EmptyLibraryLossSBATests {

        @Test
        void appliesToReturnsFalseForStub() {
            var sba = new EmptyLibraryLossSBA();

            assertThat(sba.appliesTo(gameState)).isFalse();
        }

        @Test
        void applyDoesNothingForStub() {
            var sba = new EmptyLibraryLossSBA();

            sba.apply(gameState);
        }
    }

    @Nested
    class PoisonCounterLossSBATests {

        @Test
        void appliesToReturnsFalseForStub() {
            var sba = new PoisonCounterLossSBA();

            assertThat(sba.appliesTo(gameState)).isFalse();
        }

        @Test
        void applyDoesNothingForStub() {
            var sba = new PoisonCounterLossSBA();

            sba.apply(gameState);
        }
    }

    @Nested
    class LegendRuleSBATests {

        @Test
        void appliesToReturnsFalseForStub() {
            var sba = new LegendRuleSBA();

            assertThat(sba.appliesTo(gameState)).isFalse();
        }

        @Test
        void applyDoesNothingForStub() {
            var sba = new LegendRuleSBA();

            sba.apply(gameState);
        }
    }

    @Nested
    class WorldRuleSBATests {

        @Test
        void appliesToReturnsFalseForStub() {
            var sba = new WorldRuleSBA();

            assertThat(sba.appliesTo(gameState)).isFalse();
        }

        @Test
        void applyDoesNothingForStub() {
            var sba = new WorldRuleSBA();

            sba.apply(gameState);
        }
    }

    @Nested
    class TokenCeasesToExistSBATests {

        @Test
        void appliesToReturnsFalseForStub() {
            var sba = new TokenCeasesToExistSBA();

            assertThat(sba.appliesTo(gameState)).isFalse();
        }

        @Test
        void applyDoesNothingForStub() {
            var sba = new TokenCeasesToExistSBA();

            sba.apply(gameState);
        }
    }

    @Nested
    class CopyCeasesToExistSBATests {

        @Test
        void appliesToReturnsFalseForStub() {
            var sba = new CopyCeasesToExistSBA();

            assertThat(sba.appliesTo(gameState)).isFalse();
        }

        @Test
        void applyDoesNothingForStub() {
            var sba = new CopyCeasesToExistSBA();

            sba.apply(gameState);
        }
    }

    @Nested
    class DeathtouchDamageSBATests {

        @Test
        void appliesToReturnsFalseForStub() {
            var sba = new DeathtouchDamageSBA();

            assertThat(sba.appliesTo(gameState)).isFalse();
        }

        @Test
        void applyDoesNothingForStub() {
            var sba = new DeathtouchDamageSBA();

            sba.apply(gameState);
        }
    }

    @Nested
    class IllegalAuraSBATests {

        @Test
        void appliesToReturnsFalseForStub() {
            var sba = new IllegalAuraSBA();

            assertThat(sba.appliesTo(gameState)).isFalse();
        }

        @Test
        void applyDoesNothingForStub() {
            var sba = new IllegalAuraSBA();

            sba.apply(gameState);
        }
    }

    @Nested
    class IllegalEquipmentSBATests {

        @Test
        void appliesToReturnsFalseForStub() {
            var sba = new IllegalEquipmentSBA();

            assertThat(sba.appliesTo(gameState)).isFalse();
        }

        @Test
        void applyDoesNothingForStub() {
            var sba = new IllegalEquipmentSBA();

            sba.apply(gameState);
        }
    }

    @Nested
    class IllegalAttachmentSBATests {

        @Test
        void appliesToReturnsFalseForStub() {
            var sba = new IllegalAttachmentSBA();

            assertThat(sba.appliesTo(gameState)).isFalse();
        }

        @Test
        void applyDoesNothingForStub() {
            var sba = new IllegalAttachmentSBA();

            sba.apply(gameState);
        }
    }

    @Nested
    class CounterCancellationSBATests {

        @Test
        void appliesToReturnsFalseForStub() {
            var sba = new CounterCancellationSBA();

            assertThat(sba.appliesTo(gameState)).isFalse();
        }

        @Test
        void applyDoesNothingForStub() {
            var sba = new CounterCancellationSBA();

            sba.apply(gameState);
        }
    }

    @Nested
    class CounterLimitSBATests {

        @Test
        void appliesToReturnsFalseForStub() {
            var sba = new CounterLimitSBA();

            assertThat(sba.appliesTo(gameState)).isFalse();
        }

        @Test
        void applyDoesNothingForStub() {
            var sba = new CounterLimitSBA();

            sba.apply(gameState);
        }
    }

    @Nested
    class SagaFinalChapterSBATests {

        @Test
        void appliesToReturnsFalseForStub() {
            var sba = new SagaFinalChapterSBA();

            assertThat(sba.appliesTo(gameState)).isFalse();
        }

        @Test
        void applyDoesNothingForStub() {
            var sba = new SagaFinalChapterSBA();

            sba.apply(gameState);
        }
    }

    @Nested
    class DungeonCompletedSBATests {

        @Test
        void appliesToReturnsFalseForStub() {
            var sba = new DungeonCompletedSBA();

            assertThat(sba.appliesTo(gameState)).isFalse();
        }

        @Test
        void applyDoesNothingForStub() {
            var sba = new DungeonCompletedSBA();

            sba.apply(gameState);
        }
    }

    @Nested
    class BattleNoProtectorSBATests {

        @Test
        void appliesToReturnsFalseForStub() {
            var sba = new BattleNoProtectorSBA();

            assertThat(sba.appliesTo(gameState)).isFalse();
        }

        @Test
        void applyDoesNothingForStub() {
            var sba = new BattleNoProtectorSBA();

            sba.apply(gameState);
        }
    }

    @Nested
    class BattleZeroDefenseSBATests {

        @Test
        void appliesToReturnsFalseForStub() {
            var sba = new BattleZeroDefenseSBA();

            assertThat(sba.appliesTo(gameState)).isFalse();
        }

        @Test
        void applyDoesNothingForStub() {
            var sba = new BattleZeroDefenseSBA();

            sba.apply(gameState);
        }
    }

    @Nested
    class SiegeProtectorSBATests {

        @Test
        void appliesToReturnsFalseForStub() {
            var sba = new SiegeProtectorSBA();

            assertThat(sba.appliesTo(gameState)).isFalse();
        }

        @Test
        void applyDoesNothingForStub() {
            var sba = new SiegeProtectorSBA();

            sba.apply(gameState);
        }
    }

    @Nested
    class MultipleRolesSBATests {

        @Test
        void appliesToReturnsFalseForStub() {
            var sba = new MultipleRolesSBA();

            assertThat(sba.appliesTo(gameState)).isFalse();
        }

        @Test
        void applyDoesNothingForStub() {
            var sba = new MultipleRolesSBA();

            sba.apply(gameState);
        }
    }
}
