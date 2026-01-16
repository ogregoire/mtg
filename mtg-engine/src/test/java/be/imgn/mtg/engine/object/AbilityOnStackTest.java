package be.imgn.mtg.engine.object;

import static be.imgn.mtg.engine.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.characteristics.Ability;
import be.imgn.mtg.engine.characteristics.Color;
import be.imgn.mtg.engine.characteristics.Type;
import be.imgn.mtg.engine.characteristics.Value;
import be.imgn.mtg.engine.game.Player;

class AbilityOnStackTest {

    @Test
    void abilityFromPermanent() {
        var player = mock(Player.class);
        var ability = mock(Ability.class);

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
        var ability = mock(Ability.class);

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
}
