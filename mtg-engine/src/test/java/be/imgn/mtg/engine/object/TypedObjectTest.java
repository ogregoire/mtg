package be.imgn.mtg.engine.object;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.ability.ActivatedAbility;
import be.imgn.mtg.engine.characteristics.Color;
import be.imgn.mtg.engine.characteristics.Type;
import be.imgn.mtg.engine.characteristics.Value;
import be.imgn.mtg.engine.game.Player;

class TypedObjectTest {

    @Nested
    class HierarchyTests {

        @Test
        void cardIsTypedObject() {
            var player = mock(Player.class);
            var card = Card.builder()
                    .owner(player)
                    .controller(player)
                    .name("Test Card")
                    .build();

            assertThat(card).isInstanceOf(TypedObject.class);
            assertThat(card).isInstanceOf(GameObject.class);
        }

        @Test
        void permanentIsTypedObject() {
            var player = mock(Player.class);
            var card = Card.builder()
                    .owner(player)
                    .controller(player)
                    .name("Test Card")
                    .type(Type.CREATURE)
                    .build();
            var permanent = Permanent.fromCard(card, player).build();

            assertThat(permanent).isInstanceOf(TypedObject.class);
            assertThat(permanent).isInstanceOf(GameObject.class);
        }

        @Test
        void spellIsTypedObject() {
            var player = mock(Player.class);
            var card = Card.builder()
                    .owner(player)
                    .controller(player)
                    .name("Test Spell")
                    .type(Type.INSTANT)
                    .build();
            var spell = Spell.fromCard(card, player).build();

            assertThat(spell).isInstanceOf(TypedObject.class);
            assertThat(spell).isInstanceOf(GameObject.class);
        }

        @Test
        void tokenIsTypedObject() {
            var player = mock(Player.class);
            var token = Token.builder()
                    .owner(player)
                    .controller(player)
                    .name("Test Token")
                    .type(Type.CREATURE)
                    .build();

            assertThat(token).isInstanceOf(TypedObject.class);
            assertThat(token).isInstanceOf(GameObject.class);
        }

        @Test
        void cardCopyIsTypedObject() {
            var player = mock(Player.class);
            var copy = CardCopy.builder()
                    .owner(player)
                    .controller(player)
                    .name("Copy")
                    .type(Type.INSTANT)
                    .build();

            assertThat(copy).isInstanceOf(TypedObject.class);
            assertThat(copy).isInstanceOf(GameObject.class);
        }

        @Test
        void abilityOnStackIsNotTypedObject() {
            var player = mock(Player.class);
            var ability = mock(ActivatedAbility.class);
            var card = Card.builder()
                    .owner(player)
                    .controller(player)
                    .name("Source")
                    .type(Type.CREATURE)
                    .power(Value.of(1))
                    .toughness(Value.of(1))
                    .build();
            var permanent = Permanent.fromCard(card, player).build();
            var abilityOnStack = AbilityOnStack.from(ability, permanent).build();

            assertThat(abilityOnStack).isNotInstanceOf(TypedObject.class);
            assertThat(abilityOnStack).isInstanceOf(GameObject.class);
        }

        @Test
        void emblemIsNotTypedObject() {
            var player = mock(Player.class);
            var emblem = Emblem.builder().owner(player).controller(player).build();

            assertThat(emblem).isNotInstanceOf(TypedObject.class);
            assertThat(emblem).isInstanceOf(GameObject.class);
        }
    }

    @Nested
    class CharacteristicAccess {

        @Test
        void typedObjectProvidesAllCharacteristics() {
            var player = mock(Player.class);
            TypedObject obj = Card.builder()
                    .owner(player)
                    .controller(player)
                    .name("Lightning Bolt")
                    .color(Color.RED)
                    .type(Type.INSTANT)
                    .build();

            assertThat(obj.name()).isEqualTo("Lightning Bolt");
            assertThat(obj.colors().contains(Color.RED)).isTrue();
            assertThat(obj.types().contains(Type.INSTANT)).isTrue();
            assertThat(obj.supertypes()).isNotNull();
            assertThat(obj.subtypes()).isNotNull();
            assertThat(obj.abilities()).isNotNull();
            assertThat(obj.costs()).isNotNull();
            assertThat(obj.manaValue()).isNotNull();
        }

        @Test
        void typedObjectPowerAndToughness() {
            var player = mock(Player.class);
            TypedObject creature = Card.builder()
                    .owner(player)
                    .controller(player)
                    .name("Bear")
                    .type(Type.CREATURE)
                    .power(Value.of(2))
                    .toughness(Value.of(2))
                    .build();

            assertThat(creature.power()).isNotNull();
            assertThat(requireNonNull(creature.power()).value()).isEqualTo(2);
            assertThat(creature.toughness()).isNotNull();
            assertThat(requireNonNull(creature.toughness()).value()).isEqualTo(2);
        }

        @Test
        void typedObjectNullPowerAndToughness() {
            var player = mock(Player.class);
            TypedObject instant = Card.builder()
                    .owner(player)
                    .controller(player)
                    .name("Counterspell")
                    .type(Type.INSTANT)
                    .build();

            assertThat(instant.power()).isNull();
            assertThat(instant.toughness()).isNull();
        }
    }
}
