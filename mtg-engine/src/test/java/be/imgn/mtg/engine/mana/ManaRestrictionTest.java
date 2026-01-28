package be.imgn.mtg.engine.mana;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.characteristics.Type;
import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.object.Card;

class ManaRestrictionTest {

    @Nested
    class TypeRestriction {

        @Test
        void canSpendOnMatchingType() {
            var restriction = new ManaRestriction.TypeRestriction(Type.CREATURE);
            var player = mock(Player.class);
            var creature = Card.builder()
                    .owner(player)
                    .controller(player)
                    .name("Creature")
                    .type(Type.CREATURE)
                    .build();

            assertThat(restriction.canSpendOn(creature)).isTrue();
        }

        @Test
        void cannotSpendOnNonMatchingType() {
            var restriction = new ManaRestriction.TypeRestriction(Type.CREATURE);
            var player = mock(Player.class);
            var artifact = Card.builder()
                    .owner(player)
                    .controller(player)
                    .name("Artifact")
                    .type(Type.ARTIFACT)
                    .build();

            assertThat(restriction.canSpendOn(artifact)).isFalse();
        }

        @Test
        void canSpendOnMultipleTypes() {
            var restriction = new ManaRestriction.TypeRestriction(Type.ARTIFACT);
            var player = mock(Player.class);
            var artifactCreature = Card.builder()
                    .owner(player)
                    .controller(player)
                    .name("Artifact Creature")
                    .types(Type.ARTIFACT, Type.CREATURE)
                    .build();

            assertThat(restriction.canSpendOn(artifactCreature)).isTrue();
        }

        @Test
        void instantRestriction() {
            var restriction = new ManaRestriction.TypeRestriction(Type.INSTANT);
            var player = mock(Player.class);
            var instant = Card.builder()
                    .owner(player)
                    .controller(player)
                    .name("Instant")
                    .type(Type.INSTANT)
                    .build();

            assertThat(restriction.canSpendOn(instant)).isTrue();
        }

        @Test
        void sorceryRestriction() {
            var restriction = new ManaRestriction.TypeRestriction(Type.SORCERY);
            var player = mock(Player.class);
            var sorcery = Card.builder()
                    .owner(player)
                    .controller(player)
                    .name("Sorcery")
                    .type(Type.SORCERY)
                    .build();

            assertThat(restriction.canSpendOn(sorcery)).isTrue();
        }

        @Test
        void enchantmentRestriction() {
            var restriction = new ManaRestriction.TypeRestriction(Type.ENCHANTMENT);
            var player = mock(Player.class);
            var enchantment = Card.builder()
                    .owner(player)
                    .controller(player)
                    .name("Enchantment")
                    .type(Type.ENCHANTMENT)
                    .build();

            assertThat(restriction.canSpendOn(enchantment)).isTrue();
        }

        @Test
        void planeswalkerRestriction() {
            var restriction = new ManaRestriction.TypeRestriction(Type.PLANESWALKER);
            var player = mock(Player.class);
            var planeswalker = Card.builder()
                    .owner(player)
                    .controller(player)
                    .name("Planeswalker")
                    .type(Type.PLANESWALKER)
                    .build();

            assertThat(restriction.canSpendOn(planeswalker)).isTrue();
        }

        @Test
        void landRestriction() {
            var restriction = new ManaRestriction.TypeRestriction(Type.LAND);
            var player = mock(Player.class);
            var land = Card.builder()
                    .owner(player)
                    .controller(player)
                    .name("Land")
                    .type(Type.LAND)
                    .build();

            assertThat(restriction.canSpendOn(land)).isTrue();
        }

        @Test
        void battleRestriction() {
            var restriction = new ManaRestriction.TypeRestriction(Type.BATTLE);
            var player = mock(Player.class);
            var battle = Card.builder()
                    .owner(player)
                    .controller(player)
                    .name("Battle")
                    .type(Type.BATTLE)
                    .build();

            assertThat(restriction.canSpendOn(battle)).isTrue();
        }

        @Test
        void cannotSpendOnCardWithoutTypes() {
            var restriction = new ManaRestriction.TypeRestriction(Type.CREATURE);
            var player = mock(Player.class);
            var card = Card.builder()
                    .owner(player)
                    .controller(player)
                    .name("Typeless Card")
                    .build();

            assertThat(restriction.canSpendOn(card)).isFalse();
        }
    }
}
