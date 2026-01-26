package be.imgn.mtg.engine.combat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.object.Permanent;

class DamageTargetTest {

    private final Player mockPlayer = mock(Player.class);

    @Nested
    class PlayerTargetTests {

        @Test
        void createsPlayerTarget() {
            var target = new DamageTarget.PlayerTarget(mockPlayer);

            assertThat(target).isNotNull();
            assertThat(target.player()).isEqualTo(mockPlayer);
        }

        @Test
        void affectedPlayerIsThePlayer() {
            var target = new DamageTarget.PlayerTarget(mockPlayer);

            assertThat(target.affectedPlayer()).isEqualTo(mockPlayer);
        }

        @Test
        void implementsDamageTarget() {
            var target = new DamageTarget.PlayerTarget(mockPlayer);

            assertThat(target).isInstanceOf(DamageTarget.class);
        }

        @Test
        void equalityBasedOnPlayer() {
            var target1 = new DamageTarget.PlayerTarget(mockPlayer);
            var target2 = new DamageTarget.PlayerTarget(mockPlayer);

            assertThat(target1).isEqualTo(target2);
        }

        @Test
        void differentPlayersNotEqual() {
            var player1 = mock(Player.class);
            var player2 = mock(Player.class);

            var target1 = new DamageTarget.PlayerTarget(player1);
            var target2 = new DamageTarget.PlayerTarget(player2);

            assertThat(target1).isNotEqualTo(target2);
        }
    }

    @Nested
    class CreatureTargetTests {

        @Test
        void createsCreatureTarget() {
            var creature = mock(Permanent.class);
            var target = new DamageTarget.CreatureTarget(creature);

            assertThat(target).isNotNull();
            assertThat(target.creature()).isEqualTo(creature);
        }

        @Test
        void affectedPlayerIsController() {
            var creature = mock(Permanent.class);
            when(creature.controller()).thenReturn(mockPlayer);

            var target = new DamageTarget.CreatureTarget(creature);

            assertThat(target.affectedPlayer()).isEqualTo(mockPlayer);
        }

        @Test
        void implementsDamageTarget() {
            var creature = mock(Permanent.class);
            var target = new DamageTarget.CreatureTarget(creature);

            assertThat(target).isInstanceOf(DamageTarget.class);
        }

        @Test
        void equalityBasedOnCreature() {
            var creature = mock(Permanent.class);
            var target1 = new DamageTarget.CreatureTarget(creature);
            var target2 = new DamageTarget.CreatureTarget(creature);

            assertThat(target1).isEqualTo(target2);
        }

        @Test
        void differentCreaturesNotEqual() {
            var creature1 = mock(Permanent.class);
            var creature2 = mock(Permanent.class);

            var target1 = new DamageTarget.CreatureTarget(creature1);
            var target2 = new DamageTarget.CreatureTarget(creature2);

            assertThat(target1).isNotEqualTo(target2);
        }
    }

    @Nested
    class PlaneswalkerTargetTests {

        @Test
        void createsPlaneswalkerTarget() {
            var planeswalker = mock(Permanent.class);
            var target = new DamageTarget.PlaneswalkerTarget(planeswalker);

            assertThat(target).isNotNull();
            assertThat(target.planeswalker()).isEqualTo(planeswalker);
        }

        @Test
        void affectedPlayerIsController() {
            var planeswalker = mock(Permanent.class);
            when(planeswalker.controller()).thenReturn(mockPlayer);

            var target = new DamageTarget.PlaneswalkerTarget(planeswalker);

            assertThat(target.affectedPlayer()).isEqualTo(mockPlayer);
        }

        @Test
        void implementsDamageTarget() {
            var planeswalker = mock(Permanent.class);
            var target = new DamageTarget.PlaneswalkerTarget(planeswalker);

            assertThat(target).isInstanceOf(DamageTarget.class);
        }

        @Test
        void equalityBasedOnPlaneswalker() {
            var planeswalker = mock(Permanent.class);
            var target1 = new DamageTarget.PlaneswalkerTarget(planeswalker);
            var target2 = new DamageTarget.PlaneswalkerTarget(planeswalker);

            assertThat(target1).isEqualTo(target2);
        }

        @Test
        void differentPlaneswalkersNotEqual() {
            var planeswalker1 = mock(Permanent.class);
            var planeswalker2 = mock(Permanent.class);

            var target1 = new DamageTarget.PlaneswalkerTarget(planeswalker1);
            var target2 = new DamageTarget.PlaneswalkerTarget(planeswalker2);

            assertThat(target1).isNotEqualTo(target2);
        }
    }

    @Nested
    class BattleTargetTests {

        @Test
        void createsBattleTarget() {
            var battle = mock(Permanent.class);
            var target = new DamageTarget.BattleTarget(battle);

            assertThat(target).isNotNull();
            assertThat(target.battle()).isEqualTo(battle);
        }

        @Test
        void affectedPlayerIsController() {
            var battle = mock(Permanent.class);
            when(battle.controller()).thenReturn(mockPlayer);

            var target = new DamageTarget.BattleTarget(battle);

            assertThat(target.affectedPlayer()).isEqualTo(mockPlayer);
        }

        @Test
        void implementsDamageTarget() {
            var battle = mock(Permanent.class);
            var target = new DamageTarget.BattleTarget(battle);

            assertThat(target).isInstanceOf(DamageTarget.class);
        }

        @Test
        void equalityBasedOnBattle() {
            var battle = mock(Permanent.class);
            var target1 = new DamageTarget.BattleTarget(battle);
            var target2 = new DamageTarget.BattleTarget(battle);

            assertThat(target1).isEqualTo(target2);
        }

        @Test
        void differentBattlesNotEqual() {
            var battle1 = mock(Permanent.class);
            var battle2 = mock(Permanent.class);

            var target1 = new DamageTarget.BattleTarget(battle1);
            var target2 = new DamageTarget.BattleTarget(battle2);

            assertThat(target1).isNotEqualTo(target2);
        }
    }

    @Nested
    class SealedInterfaceTests {

        @Test
        void playerTargetIsValidImplementation() {
            var target = new DamageTarget.PlayerTarget(mockPlayer);

            assertThat(target).isInstanceOf(DamageTarget.class);
        }

        @Test
        void creatureTargetIsValidImplementation() {
            var creature = mock(Permanent.class);
            var target = new DamageTarget.CreatureTarget(creature);

            assertThat(target).isInstanceOf(DamageTarget.class);
        }

        @Test
        void planeswalkerTargetIsValidImplementation() {
            var planeswalker = mock(Permanent.class);
            var target = new DamageTarget.PlaneswalkerTarget(planeswalker);

            assertThat(target).isInstanceOf(DamageTarget.class);
        }

        @Test
        void battleTargetIsValidImplementation() {
            var battle = mock(Permanent.class);
            var target = new DamageTarget.BattleTarget(battle);

            assertThat(target).isInstanceOf(DamageTarget.class);
        }
    }

    @Nested
    class DifferentTargetTypeComparisons {

        @Test
        void playerTargetNotEqualToCreatureTarget() {
            var creature = mock(Permanent.class);
            var playerTarget = new DamageTarget.PlayerTarget(mockPlayer);
            var creatureTarget = new DamageTarget.CreatureTarget(creature);

            assertThat(playerTarget).isNotEqualTo(creatureTarget);
        }

        @Test
        void creatureTargetNotEqualToPlaneswalkerTarget() {
            var creature = mock(Permanent.class);
            var planeswalker = mock(Permanent.class);
            var creatureTarget = new DamageTarget.CreatureTarget(creature);
            var planeswalkerTarget = new DamageTarget.PlaneswalkerTarget(planeswalker);

            assertThat(creatureTarget).isNotEqualTo(planeswalkerTarget);
        }

        @Test
        void planeswalkerTargetNotEqualToBattleTarget() {
            var planeswalker = mock(Permanent.class);
            var battle = mock(Permanent.class);
            var planeswalkerTarget = new DamageTarget.PlaneswalkerTarget(planeswalker);
            var battleTarget = new DamageTarget.BattleTarget(battle);

            assertThat(planeswalkerTarget).isNotEqualTo(battleTarget);
        }

        @Test
        void battleTargetNotEqualToPlayerTarget() {
            var battle = mock(Permanent.class);
            var battleTarget = new DamageTarget.BattleTarget(battle);
            var playerTarget = new DamageTarget.PlayerTarget(mockPlayer);

            assertThat(battleTarget).isNotEqualTo(playerTarget);
        }
    }
}
