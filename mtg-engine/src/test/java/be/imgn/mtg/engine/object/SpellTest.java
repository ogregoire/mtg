package be.imgn.mtg.engine.object;

import static be.imgn.mtg.engine.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.characteristics.Color;
import be.imgn.mtg.engine.characteristics.CreatureType;
import be.imgn.mtg.engine.characteristics.Type;
import be.imgn.mtg.engine.characteristics.Value;
import be.imgn.mtg.engine.game.Player;

class SpellTest {

    @Test
    void spellFromCreatureCard() {
        var player = mock(Player.class);

        var card = Card.builder()
                .owner(player)
                .controller(player)
                .name("Grizzly Bears")
                .color(Color.GREEN)
                .type(Type.CREATURE)
                .subtype(CreatureType.BEAR)
                .power(Value.of(2))
                .toughness(Value.of(2))
                .build();

        var spell = Spell.fromCard(card, player).build();

        assertThat(spell)
                .hasName("Grizzly Bears")
                .hasController(player)
                .isGreen()
                .isCreature()
                .hasPower(2)
                .hasToughness(2);
    }

    @Test
    void spellFromInstantCard() {
        var player = mock(Player.class);

        var card = Card.builder()
                .owner(player)
                .controller(player)
                .name("Lightning Bolt")
                .color(Color.RED)
                .type(Type.INSTANT)
                .rulesText("Lightning Bolt deals 3 damage to any target.")
                .build();

        var spell = Spell.fromCard(card, player).build();

        assertThat(spell).hasName("Lightning Bolt").isInstant().isRed();
    }

    @Test
    void spellFromCardCopy() {
        var player = mock(Player.class);

        var card = Card.builder()
                .owner(player)
                .controller(player)
                .name("Counterspell")
                .color(Color.BLUE)
                .type(Type.INSTANT)
                .build();

        var copy = CardCopy.builder()
                .owner(player)
                .controller(player)
                .original(card)
                .name("Counterspell")
                .color(Color.BLUE)
                .type(Type.INSTANT)
                .build();

        var spell = Spell.fromCopy(copy, player).build();

        assertThat(spell).hasName("Counterspell").isInstant().isBlue();
    }
}
