package be.imgn.mtg.engine.object;

import static be.imgn.mtg.engine.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.characteristics.Color;
import be.imgn.mtg.engine.characteristics.CreatureType;
import be.imgn.mtg.engine.characteristics.Supertype;
import be.imgn.mtg.engine.characteristics.Type;
import be.imgn.mtg.engine.characteristics.Value;
import be.imgn.mtg.engine.game.Player;

class CardTest {

    @Test
    void basicCreatureCard() {
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

        assertThat(card)
                .hasName("Grizzly Bears")
                .hasOwner(player)
                .hasController(player)
                .isGreen()
                .isCreature()
                .hasPower(2)
                .hasToughness(2)
                .hasManaValue(0); // No mana cost set
    }

    @Test
    void legendaryCreatureCard() {
        var player = mock(Player.class);

        var card = Card.builder()
                .owner(player)
                .controller(player)
                .name("Ragavan, Nimble Pilferer")
                .color(Color.RED)
                .type(Type.CREATURE)
                .supertype(Supertype.LEGENDARY)
                .subtypes(CreatureType.MONKEY, CreatureType.PIRATE)
                .power(Value.of(2))
                .toughness(Value.of(1))
                .build();

        assertThat(card)
                .hasName("Ragavan, Nimble Pilferer")
                .isLegendary()
                .isCreature()
                .isRed();
    }

    @Test
    void artifactCreatureCard() {
        var player = mock(Player.class);

        var card = Card.builder()
                .owner(player)
                .controller(player)
                .name("Myr Battlesphere")
                .types(Type.ARTIFACT, Type.CREATURE)
                .subtypes(CreatureType.MYR, CreatureType.CONSTRUCT)
                .power(Value.of(4))
                .toughness(Value.of(7))
                .build();

        assertThat(card).isArtifact().isCreature().isColorless();
    }

    @Test
    void instantCard() {
        var player = mock(Player.class);

        var card = Card.builder()
                .owner(player)
                .controller(player)
                .name("Lightning Bolt")
                .color(Color.RED)
                .type(Type.INSTANT)
                .rulesText("Lightning Bolt deals 3 damage to any target.")
                .build();

        assertThat(card)
                .hasName("Lightning Bolt")
                .isInstant()
                .isRed()
                .hasRulesText("Lightning Bolt deals 3 damage to any target.");
    }

    @Test
    void landCard() {
        var player = mock(Player.class);

        var card = Card.builder()
                .owner(player)
                .controller(player)
                .name("Forest")
                .type(Type.LAND)
                .supertype(Supertype.BASIC)
                .build();

        assertThat(card).hasName("Forest").isLand().isBasic().isColorless().hasNoManaCost();
    }

    @Test
    void multiColoredCard() {
        var player = mock(Player.class);

        var card = Card.builder()
                .owner(player)
                .controller(player)
                .name("Nicol Bolas, the Ravager")
                .colors(Color.BLUE, Color.BLACK, Color.RED)
                .type(Type.CREATURE)
                .supertype(Supertype.LEGENDARY)
                .subtypes(CreatureType.ELDER, CreatureType.DRAGON)
                .power(Value.of(4))
                .toughness(Value.of(4))
                .build();

        assertThat(card).isMultiColored().isBlue().isBlack().isRed();
    }

    @Test
    void cardWithNoRulesText() {
        var player = mock(Player.class);

        var card = Card.builder()
                .owner(player)
                .controller(player)
                .name("Vanilla Creature")
                .type(Type.CREATURE)
                .power(Value.of(2))
                .toughness(Value.of(2))
                .build();

        assertThat(card).hasEmptyRulesText();
    }
}
