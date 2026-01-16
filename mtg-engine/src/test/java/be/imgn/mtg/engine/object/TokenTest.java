package be.imgn.mtg.engine.object;

import static be.imgn.mtg.engine.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.characteristics.ArtifactType;
import be.imgn.mtg.engine.characteristics.Color;
import be.imgn.mtg.engine.characteristics.CreatureType;
import be.imgn.mtg.engine.characteristics.Type;
import be.imgn.mtg.engine.characteristics.Value;

class TokenTest {

    @Test
    void basicCreatureToken() {
        var player = mock(Player.class);

        var token = Token.builder()
                .owner(player)
                .controller(player)
                .name("Soldier Token")
                .color(Color.WHITE)
                .type(Type.CREATURE)
                .subtype(CreatureType.SOLDIER)
                .power(Value.of(1))
                .toughness(Value.of(1))
                .build();

        assertThat(token)
                .hasName("Soldier Token")
                .hasOwner(player)
                .hasController(player)
                .isWhite()
                .isCreature()
                .hasPower(1)
                .hasToughness(1);
    }

    @Test
    void colorlessArtifactToken() {
        var player = mock(Player.class);

        var token = Token.builder()
                .owner(player)
                .controller(player)
                .name("Treasure")
                .type(Type.ARTIFACT)
                .subtype(ArtifactType.TREASURE)
                .build();

        assertThat(token).hasName("Treasure").isArtifact().isColorless();
    }

    @Test
    void multiColoredToken() {
        var player = mock(Player.class);

        var token = Token.builder()
                .owner(player)
                .controller(player)
                .name("Elemental Token")
                .colors(Color.RED, Color.GREEN)
                .type(Type.CREATURE)
                .subtype(CreatureType.ELEMENTAL)
                .power(Value.of(5))
                .toughness(Value.of(5))
                .build();

        assertThat(token).isMultiColored().isRed().isGreen().hasPower(5).hasToughness(5);
    }
}
