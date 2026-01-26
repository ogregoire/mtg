package be.imgn.mtg.engine.combat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.event.GameEvent;
import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.object.Permanent;

class DamageEventTest {

    private final Permanent mockSource = mock(Permanent.class);
    private final Player mockPlayer = mock(Player.class);

    @Nested
    class BasicEventCreation {

        @Test
        void createsEventWithAllFields() {
            var target = new DamageTarget.PlayerTarget(mockPlayer);
            var event = new DamageEvent(mockSource, target, 3, true, false);

            assertThat(event.source()).isEqualTo(mockSource);
            assertThat(event.target()).isEqualTo(target);
            assertThat(event.amount()).isEqualTo(3);
            assertThat(event.combat()).isTrue();
            assertThat(event.infect()).isFalse();
        }

        @Test
        void implementsGameEvent() {
            var target = new DamageTarget.PlayerTarget(mockPlayer);
            var event = new DamageEvent(mockSource, target, 1, false, false);

            assertThat(event).isInstanceOf(GameEvent.class);
        }

        @Test
        void affectedPlayerDelegatesToTarget() {
            var target = new DamageTarget.PlayerTarget(mockPlayer);
            var event = new DamageEvent(mockSource, target, 2, false, false);

            assertThat(event.affectedPlayer()).isEqualTo(mockPlayer);
        }

        @Test
        void affectedPlayerFromCreatureTarget() {
            var creature = mock(Permanent.class);
            when(creature.controller()).thenReturn(mockPlayer);
            var target = new DamageTarget.CreatureTarget(creature);
            var event = new DamageEvent(mockSource, target, 5, false, false);

            assertThat(event.affectedPlayer()).isEqualTo(mockPlayer);
        }
    }

    @Nested
    class CombatDamageTests {

        @Test
        void combatDamageReturnsTrueForCombatFlag() {
            var target = new DamageTarget.PlayerTarget(mockPlayer);
            var event = new DamageEvent(mockSource, target, 3, true, false);

            assertThat(event.isCombatDamage()).isTrue();
            assertThat(event.isNonCombatDamage()).isFalse();
        }

        @Test
        void nonCombatDamageReturnsFalseForCombatFlag() {
            var target = new DamageTarget.PlayerTarget(mockPlayer);
            var event = new DamageEvent(mockSource, target, 2, false, false);

            assertThat(event.isCombatDamage()).isFalse();
            assertThat(event.isNonCombatDamage()).isTrue();
        }

        @Test
        void combatAccessorMatchesConstructor() {
            var target = new DamageTarget.PlayerTarget(mockPlayer);
            var combatEvent = new DamageEvent(mockSource, target, 1, true, false);
            var nonCombatEvent = new DamageEvent(mockSource, target, 1, false, false);

            assertThat(combatEvent.combat()).isTrue();
            assertThat(nonCombatEvent.combat()).isFalse();
        }
    }

    @Nested
    class InfectDamageTests {

        @Test
        void infectDamageStoresFlag() {
            var target = new DamageTarget.PlayerTarget(mockPlayer);
            var event = new DamageEvent(mockSource, target, 1, false, true);

            assertThat(event.infect()).isTrue();
        }

        @Test
        void normalDamageHasNoInfect() {
            var target = new DamageTarget.PlayerTarget(mockPlayer);
            var event = new DamageEvent(mockSource, target, 1, false, false);

            assertThat(event.infect()).isFalse();
        }

        @Test
        void infectAndCombatCanBothBeTrue() {
            var target = new DamageTarget.PlayerTarget(mockPlayer);
            var event = new DamageEvent(mockSource, target, 2, true, true);

            assertThat(event.combat()).isTrue();
            assertThat(event.infect()).isTrue();
            assertThat(event.isCombatDamage()).isTrue();
        }
    }

    @Nested
    class WithAmountModification {

        @Test
        void createsNewEventWithModifiedAmount() {
            var target = new DamageTarget.PlayerTarget(mockPlayer);
            var original = new DamageEvent(mockSource, target, 3, true, false);
            var modified = original.withAmount(5);

            assertThat(modified.amount()).isEqualTo(5);
            assertThat(original.amount()).isEqualTo(3); // Original unchanged
        }

        @Test
        void preservesSourceAndTarget() {
            var target = new DamageTarget.PlayerTarget(mockPlayer);
            var original = new DamageEvent(mockSource, target, 2, false, true);
            var modified = original.withAmount(4);

            assertThat(modified.source()).isEqualTo(mockSource);
            assertThat(modified.target()).isEqualTo(target);
        }

        @Test
        void preservesCombatFlag() {
            var target = new DamageTarget.PlayerTarget(mockPlayer);
            var combatEvent = new DamageEvent(mockSource, target, 1, true, false);
            var modified = combatEvent.withAmount(3);

            assertThat(modified.combat()).isTrue();
            assertThat(modified.isCombatDamage()).isTrue();
        }

        @Test
        void preservesInfectFlag() {
            var target = new DamageTarget.PlayerTarget(mockPlayer);
            var infectEvent = new DamageEvent(mockSource, target, 1, false, true);
            var modified = infectEvent.withAmount(2);

            assertThat(modified.infect()).isTrue();
        }

        @Test
        void canIncreaseAmount() {
            var target = new DamageTarget.PlayerTarget(mockPlayer);
            var event = new DamageEvent(mockSource, target, 2, false, false);
            var increased = event.withAmount(5);

            assertThat(increased.amount()).isEqualTo(5);
        }

        @Test
        void canDecreaseAmount() {
            var target = new DamageTarget.PlayerTarget(mockPlayer);
            var event = new DamageEvent(mockSource, target, 5, false, false);
            var decreased = event.withAmount(1);

            assertThat(decreased.amount()).isEqualTo(1);
        }

        @Test
        void canSetAmountToZero() {
            var target = new DamageTarget.PlayerTarget(mockPlayer);
            var event = new DamageEvent(mockSource, target, 3, false, false);
            var prevented = event.withAmount(0);

            assertThat(prevented.amount()).isEqualTo(0);
        }

        @Test
        void canSetNegativeAmount() {
            var target = new DamageTarget.PlayerTarget(mockPlayer);
            var event = new DamageEvent(mockSource, target, 3, false, false);
            var negative = event.withAmount(-1);

            assertThat(negative.amount()).isEqualTo(-1);
        }
    }

    @Nested
    class DifferentTargetTypes {

        @Test
        void damageToPlayer() {
            var target = new DamageTarget.PlayerTarget(mockPlayer);
            var event = new DamageEvent(mockSource, target, 3, false, false);

            assertThat(event.target()).isInstanceOf(DamageTarget.PlayerTarget.class);
            assertThat(event.affectedPlayer()).isEqualTo(mockPlayer);
        }

        @Test
        void damageToCreature() {
            var creature = mock(Permanent.class);
            when(creature.controller()).thenReturn(mockPlayer);
            var target = new DamageTarget.CreatureTarget(creature);
            var event = new DamageEvent(mockSource, target, 4, true, false);

            assertThat(event.target()).isInstanceOf(DamageTarget.CreatureTarget.class);
            assertThat(event.affectedPlayer()).isEqualTo(mockPlayer);
        }

        @Test
        void damageToPlaneswalker() {
            var planeswalker = mock(Permanent.class);
            when(planeswalker.controller()).thenReturn(mockPlayer);
            var target = new DamageTarget.PlaneswalkerTarget(planeswalker);
            var event = new DamageEvent(mockSource, target, 2, false, false);

            assertThat(event.target()).isInstanceOf(DamageTarget.PlaneswalkerTarget.class);
            assertThat(event.affectedPlayer()).isEqualTo(mockPlayer);
        }

        @Test
        void damageToBattle() {
            var battle = mock(Permanent.class);
            when(battle.controller()).thenReturn(mockPlayer);
            var target = new DamageTarget.BattleTarget(battle);
            var event = new DamageEvent(mockSource, target, 5, true, false);

            assertThat(event.target()).isInstanceOf(DamageTarget.BattleTarget.class);
            assertThat(event.affectedPlayer()).isEqualTo(mockPlayer);
        }
    }

    @Nested
    class EqualityTests {

        @Test
        void sameFieldsAreEqual() {
            var target = new DamageTarget.PlayerTarget(mockPlayer);
            var event1 = new DamageEvent(mockSource, target, 3, true, false);
            var event2 = new DamageEvent(mockSource, target, 3, true, false);

            assertThat(event1).isEqualTo(event2);
        }

        @Test
        void differentAmountsNotEqual() {
            var target = new DamageTarget.PlayerTarget(mockPlayer);
            var event1 = new DamageEvent(mockSource, target, 3, false, false);
            var event2 = new DamageEvent(mockSource, target, 5, false, false);

            assertThat(event1).isNotEqualTo(event2);
        }

        @Test
        void differentCombatFlagsNotEqual() {
            var target = new DamageTarget.PlayerTarget(mockPlayer);
            var event1 = new DamageEvent(mockSource, target, 3, true, false);
            var event2 = new DamageEvent(mockSource, target, 3, false, false);

            assertThat(event1).isNotEqualTo(event2);
        }

        @Test
        void differentInfectFlagsNotEqual() {
            var target = new DamageTarget.PlayerTarget(mockPlayer);
            var event1 = new DamageEvent(mockSource, target, 2, false, true);
            var event2 = new DamageEvent(mockSource, target, 2, false, false);

            assertThat(event1).isNotEqualTo(event2);
        }

        @Test
        void differentTargetsNotEqual() {
            var player1 = mock(Player.class);
            var player2 = mock(Player.class);
            var target1 = new DamageTarget.PlayerTarget(player1);
            var target2 = new DamageTarget.PlayerTarget(player2);
            var event1 = new DamageEvent(mockSource, target1, 2, false, false);
            var event2 = new DamageEvent(mockSource, target2, 2, false, false);

            assertThat(event1).isNotEqualTo(event2);
        }

        @Test
        void differentSourcesNotEqual() {
            var source1 = mock(Permanent.class);
            var source2 = mock(Permanent.class);
            var target = new DamageTarget.PlayerTarget(mockPlayer);
            var event1 = new DamageEvent(source1, target, 2, false, false);
            var event2 = new DamageEvent(source2, target, 2, false, false);

            assertThat(event1).isNotEqualTo(event2);
        }
    }

    @Nested
    class EdgeCases {

        @Test
        void zeroDamage() {
            var target = new DamageTarget.PlayerTarget(mockPlayer);
            var event = new DamageEvent(mockSource, target, 0, false, false);

            assertThat(event.amount()).isEqualTo(0);
        }

        @Test
        void negativeDamage() {
            var target = new DamageTarget.PlayerTarget(mockPlayer);
            var event = new DamageEvent(mockSource, target, -5, false, false);

            assertThat(event.amount()).isEqualTo(-5);
        }

        @Test
        void largeDamageAmount() {
            var target = new DamageTarget.PlayerTarget(mockPlayer);
            var event = new DamageEvent(mockSource, target, Integer.MAX_VALUE, false, false);

            assertThat(event.amount()).isEqualTo(Integer.MAX_VALUE);
        }

        @Test
        void combatInfectDamage() {
            var target = new DamageTarget.PlayerTarget(mockPlayer);
            var event = new DamageEvent(mockSource, target, 1, true, true);

            assertThat(event.isCombatDamage()).isTrue();
            assertThat(event.isNonCombatDamage()).isFalse();
            assertThat(event.infect()).isTrue();
        }
    }
}
