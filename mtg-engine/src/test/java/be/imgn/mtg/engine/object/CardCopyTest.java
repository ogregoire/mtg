package be.imgn.mtg.engine.object;

import static be.imgn.mtg.engine.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.characteristics.Color;
import be.imgn.mtg.engine.characteristics.Type;

class CardCopyTest {

    @Test
    void copyOfInstantCard() {
        var player = mock(Player.class);

        var original = Card.builder()
                .owner(player)
                .controller(player)
                .name("Lightning Bolt")
                .color(Color.RED)
                .type(Type.INSTANT)
                .rulesText("Lightning Bolt deals 3 damage to any target.")
                .build();

        var copy = CardCopy.builder()
                .owner(player)
                .controller(player)
                .original(original)
                .name("Lightning Bolt")
                .color(Color.RED)
                .type(Type.INSTANT)
                .build();

        assertThat(copy)
                .hasName("Lightning Bolt")
                .hasOwner(player)
                .hasController(player)
                .isInstant()
                .isRed();

        Assertions.assertThat(copy.original()).isSameAs(original);
    }

    @Test
    void copyWithDifferentController() {
        var owner = mock(Player.class);
        var controller = mock(Player.class);

        var original = Card.builder()
                .owner(owner)
                .controller(owner)
                .name("Sorcery")
                .color(Color.BLUE)
                .type(Type.SORCERY)
                .build();

        var copy = CardCopy.builder()
                .owner(owner)
                .controller(controller)
                .original(original)
                .name("Sorcery")
                .color(Color.BLUE)
                .type(Type.SORCERY)
                .build();

        assertThat(copy).hasOwner(owner).hasController(controller);
    }
}
