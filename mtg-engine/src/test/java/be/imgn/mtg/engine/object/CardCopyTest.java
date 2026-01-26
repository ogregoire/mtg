package be.imgn.mtg.engine.object;

import static be.imgn.mtg.engine.assertions.MTGAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.characteristics.Color;
import be.imgn.mtg.engine.characteristics.CreatureType;
import be.imgn.mtg.engine.characteristics.Type;
import be.imgn.mtg.engine.characteristics.Value;
import be.imgn.mtg.engine.game.Player;

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

        assertThat(copy.original()).isSameAs(original);
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

    @Nested
    class CopyProperties {

        @Test
        void copyRetainsOriginalReference() {
            var player = mock(Player.class);

            var original = Card.builder()
                    .owner(player)
                    .controller(player)
                    .name("Original")
                    .type(Type.INSTANT)
                    .build();

            var copy = CardCopy.builder()
                    .owner(player)
                    .controller(player)
                    .original(original)
                    .name("Original")
                    .type(Type.INSTANT)
                    .build();

            assertThat(copy.original()).isSameAs(original);
        }

        @Test
        void copyHasUniqueId() {
            var player = mock(Player.class);

            var original = Card.builder()
                    .owner(player)
                    .controller(player)
                    .name("Card")
                    .type(Type.INSTANT)
                    .build();

            var copy = CardCopy.builder()
                    .owner(player)
                    .controller(player)
                    .original(original)
                    .name("Card")
                    .type(Type.INSTANT)
                    .build();

            assertThat(copy.id()).isNotEqualTo(original.id());
        }

        @Test
        void copyCanHaveDifferentCharacteristics() {
            var player = mock(Player.class);

            var original = Card.builder()
                    .owner(player)
                    .controller(player)
                    .name("Original Creature")
                    .color(Color.RED)
                    .type(Type.CREATURE)
                    .subtype(CreatureType.GOBLIN)
                    .power(Value.of(2))
                    .toughness(Value.of(2))
                    .build();

            // Copy can be modified
            var copy = CardCopy.builder()
                    .owner(player)
                    .controller(player)
                    .original(original)
                    .name("Modified Copy")
                    .colors(Color.RED, Color.WHITE)
                    .type(Type.CREATURE)
                    .subtypes(CreatureType.GOBLIN, CreatureType.SOLDIER)
                    .power(Value.of(3))
                    .toughness(Value.of(3))
                    .build();

            assertThat(copy.name()).isEqualTo("Modified Copy");
            assertThat(copy).isRed().isWhite();
            assertThat(copy.power()).isEqualTo(Value.of(3));
            assertThat(copy.toughness()).isEqualTo(Value.of(3));
        }
    }

    @Nested
    class BuilderPatterns {

        @Test
        void builderSupportsMethodChaining() {
            var player = mock(Player.class);

            var original = Card.builder()
                    .owner(player)
                    .controller(player)
                    .name("Original")
                    .type(Type.INSTANT)
                    .build();

            var copy = CardCopy.builder()
                    .owner(player)
                    .controller(player)
                    .original(original)
                    .name("Copy")
                    .type(Type.INSTANT)
                    .color(Color.BLUE)
                    .build();

            assertThat(copy.name()).isEqualTo("Copy");
        }

        @Test
        void multipleCopiesOfSameCard() {
            var player = mock(Player.class);

            var original = Card.builder()
                    .owner(player)
                    .controller(player)
                    .name("Original")
                    .type(Type.INSTANT)
                    .build();

            var copy1 = CardCopy.builder()
                    .owner(player)
                    .controller(player)
                    .original(original)
                    .name("Original")
                    .type(Type.INSTANT)
                    .build();

            var copy2 = CardCopy.builder()
                    .owner(player)
                    .controller(player)
                    .original(original)
                    .name("Original")
                    .type(Type.INSTANT)
                    .build();

            assertThat(copy1.id()).isNotEqualTo(copy2.id());
            assertThat(copy1.original()).isSameAs(original);
            assertThat(copy2.original()).isSameAs(original);
        }
    }
}
