package be.imgn.mtg.engine.object;

import static be.imgn.mtg.engine.assertions.MTGAssertions.assertThat;
import static org.mockito.Mockito.mock;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.ability.ActivatedAbility;
import be.imgn.mtg.engine.characteristics.Color;
import be.imgn.mtg.engine.characteristics.Type;
import be.imgn.mtg.engine.characteristics.Value;
import be.imgn.mtg.engine.game.Player;

class AbilityOnStackTest {

    @Test
    void abilityFromPermanent() {
        var player = mock(Player.class);
        var ability = mock(ActivatedAbility.class);

        var card = Card.builder()
                .owner(player)
                .controller(player)
                .name("Prodigal Sorcerer")
                .color(Color.BLUE)
                .type(Type.CREATURE)
                .power(Value.of(1))
                .toughness(Value.of(1))
                .build();

        var permanent = Permanent.fromCard(card, player).build();

        var abilityOnStack = AbilityOnStack.from(ability, permanent).build();

        assertThat(abilityOnStack).hasOwner(player).hasController(player);

        Assertions.assertThat(abilityOnStack.ability()).isSameAs(ability);
        Assertions.assertThat(abilityOnStack.source()).isSameAs(permanent);
    }

    @Test
    void abilityInheritsControllerFromSource() {
        var owner = mock(Player.class);
        var controller = mock(Player.class);
        var ability = mock(ActivatedAbility.class);

        var card = Card.builder()
                .owner(owner)
                .controller(owner)
                .name("Source Card")
                .type(Type.CREATURE)
                .build();

        // Permanent with different controller (e.g., stolen via Control Magic)
        var source = Permanent.fromCard(card, controller).build();

        var abilityOnStack = AbilityOnStack.from(ability, source).build();

        // Owner comes from card, controller from the permanent's controller
        assertThat(abilityOnStack).hasOwner(owner).hasController(controller);
    }

    @Nested
    class AbilitySource {

        @Test
        void abilityRetainsSourceReference() {
            var player = mock(Player.class);
            var ability = mock(ActivatedAbility.class);

            var card = Card.builder()
                    .owner(player)
                    .controller(player)
                    .name("Source")
                    .type(Type.CREATURE)
                    .build();

            var permanent = Permanent.fromCard(card, player).build();
            var abilityOnStack = AbilityOnStack.from(ability, permanent).build();

            Assertions.assertThat(abilityOnStack.source()).isSameAs(permanent);
            Assertions.assertThat(abilityOnStack.source()).isInstanceOf(Permanent.class);
        }

        @Test
        void abilityRetainsAbilityReference() {
            var player = mock(Player.class);
            var ability = mock(ActivatedAbility.class);

            var card = Card.builder()
                    .owner(player)
                    .controller(player)
                    .name("Source")
                    .type(Type.CREATURE)
                    .build();

            var permanent = Permanent.fromCard(card, player).build();
            var abilityOnStack = AbilityOnStack.from(ability, permanent).build();

            Assertions.assertThat(abilityOnStack.ability()).isSameAs(ability);
        }
    }

    @Nested
    class AbilityIdentity {

        @Test
        void eachAbilityGetsUniqueId() {
            var player = mock(Player.class);
            var ability = mock(ActivatedAbility.class);

            var card = Card.builder()
                    .owner(player)
                    .controller(player)
                    .name("Source")
                    .type(Type.CREATURE)
                    .build();

            var permanent = Permanent.fromCard(card, player).build();
            var ability1 = AbilityOnStack.from(ability, permanent).build();
            var ability2 = AbilityOnStack.from(ability, permanent).build();

            Assertions.assertThat(ability1).isNotSameAs(ability2);
        }

        @Test
        void abilityOnStackIsNotSameAsSource() {
            var player = mock(Player.class);
            var ability = mock(ActivatedAbility.class);

            var card = Card.builder()
                    .owner(player)
                    .controller(player)
                    .name("Source")
                    .type(Type.CREATURE)
                    .build();

            var permanent = Permanent.fromCard(card, player).build();
            var abilityOnStack = AbilityOnStack.from(ability, permanent).build();

            Assertions.assertThat(abilityOnStack).isNotSameAs(permanent);
        }
    }

    @Nested
    class StackObjectInterface {

        @Test
        void abilityIsStackObject() {
            var player = mock(Player.class);
            var ability = mock(ActivatedAbility.class);

            var card = Card.builder()
                    .owner(player)
                    .controller(player)
                    .name("Source")
                    .type(Type.CREATURE)
                    .build();

            var permanent = Permanent.fromCard(card, player).build();
            var abilityOnStack = AbilityOnStack.from(ability, permanent).build();

            assertThat(abilityOnStack).isInstanceOf(StackObject.class);
        }
    }

    @Nested
    class AbilityCharacteristics {

        @Test
        void abilityOnStackIsNotTypedObject() {
            var player = mock(Player.class);
            var ability = mock(ActivatedAbility.class);

            var card = Card.builder()
                    .owner(player)
                    .controller(player)
                    .name("Source")
                    .color(Color.RED)
                    .type(Type.CREATURE)
                    .power(Value.of(2))
                    .toughness(Value.of(2))
                    .build();

            var permanent = Permanent.fromCard(card, player).build();
            var abilityOnStack = AbilityOnStack.from(ability, permanent).build();

            // AbilityOnStack is a GameObject but NOT a TypedObject — it has no characteristics
            Assertions.assertThat(abilityOnStack).isInstanceOf(GameObject.class);
            Assertions.assertThat(abilityOnStack).isNotInstanceOf(TypedObject.class);
            Assertions.assertThat(abilityOnStack.name()).isEmpty();
        }
    }
}
