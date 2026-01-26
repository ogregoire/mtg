package be.imgn.mtg.engine.object;

import static be.imgn.mtg.engine.assertions.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Nested;
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

    @Nested
    class SpellSource {

        @Test
        void spellFromCardRetainsCardAsSource() {
            var player = mock(Player.class);

            var card = Card.builder()
                    .owner(player)
                    .controller(player)
                    .name("Test Card")
                    .type(Type.INSTANT)
                    .build();

            var spell = Spell.fromCard(card, player).build();

            assertThat(spell.source()).isSameAs(card);
            assertThat(spell.source()).isInstanceOf(Card.class);
        }

        @Test
        void spellFromCopyRetainsCopyAsSource() {
            var player = mock(Player.class);

            var card = Card.builder()
                    .owner(player)
                    .controller(player)
                    .name("Original")
                    .type(Type.INSTANT)
                    .build();

            var copy = CardCopy.builder()
                    .owner(player)
                    .controller(player)
                    .original(card)
                    .name("Original")
                    .type(Type.INSTANT)
                    .build();

            var spell = Spell.fromCopy(copy, player).build();

            assertThat(spell.source()).isSameAs(copy);
            assertThat(spell.source()).isInstanceOf(CardCopy.class);
        }
    }

    @Nested
    class SpellController {

        @Test
        void spellControllerCanDifferFromCardOwner() {
            var owner = mock(Player.class);
            var caster = mock(Player.class);

            var card = Card.builder()
                    .owner(owner)
                    .controller(owner)
                    .name("Stolen Spell")
                    .type(Type.INSTANT)
                    .build();

            var spell = Spell.fromCard(card, caster).build();

            assertThat(spell.owner()).isSameAs(owner);
            assertThat(spell.controller()).isSameAs(caster);
        }

        @Test
        void spellFromCopyUsesProvidedController() {
            var owner = mock(Player.class);
            var caster = mock(Player.class);

            var card = Card.builder()
                    .owner(owner)
                    .controller(owner)
                    .name("Original")
                    .type(Type.INSTANT)
                    .build();

            var copy = CardCopy.builder()
                    .owner(owner)
                    .controller(owner)
                    .original(card)
                    .name("Original")
                    .type(Type.INSTANT)
                    .build();

            var spell = Spell.fromCopy(copy, caster).build();

            assertThat(spell.controller()).isSameAs(caster);
        }
    }

    @Nested
    class SpellIdentity {

        @Test
        void eachSpellGetsUniqueId() {
            var player = mock(Player.class);

            var card = Card.builder()
                    .owner(player)
                    .controller(player)
                    .name("Card")
                    .type(Type.INSTANT)
                    .build();

            var spell1 = Spell.fromCard(card, player).build();
            var spell2 = Spell.fromCard(card, player).build();

            assertThat(spell1.id()).isNotEqualTo(spell2.id());
        }

        @Test
        void spellIdDiffersFromSourceCardId() {
            var player = mock(Player.class);

            var card = Card.builder()
                    .owner(player)
                    .controller(player)
                    .name("Card")
                    .type(Type.INSTANT)
                    .build();

            var spell = Spell.fromCard(card, player).build();

            assertThat(spell.id()).isNotEqualTo(card.id());
        }
    }

    @Nested
    class PermanentSpells {

        @Test
        void creatureSpell() {
            var player = mock(Player.class);

            var card = Card.builder()
                    .owner(player)
                    .controller(player)
                    .name("Llanowar Elves")
                    .color(Color.GREEN)
                    .type(Type.CREATURE)
                    .subtype(CreatureType.ELF)
                    .power(Value.of(1))
                    .toughness(Value.of(1))
                    .build();

            var spell = Spell.fromCard(card, player).build();

            assertThat(spell).isCreature().isGreen();
            assertThat(spell.power()).isEqualTo(Value.of(1));
            assertThat(spell.toughness()).isEqualTo(Value.of(1));
        }

        @Test
        void artifactSpell() {
            var player = mock(Player.class);

            var card = Card.builder()
                    .owner(player)
                    .controller(player)
                    .name("Sol Ring")
                    .type(Type.ARTIFACT)
                    .build();

            var spell = Spell.fromCard(card, player).build();

            assertThat(spell).isArtifact().isColorless();
        }

        @Test
        void enchantmentSpell() {
            var player = mock(Player.class);

            var card = Card.builder()
                    .owner(player)
                    .controller(player)
                    .name("Enchantment")
                    .color(Color.WHITE)
                    .type(Type.ENCHANTMENT)
                    .build();

            var spell = Spell.fromCard(card, player).build();

            assertThat(spell).isEnchantment().isWhite();
        }
    }

    @Nested
    class NonPermanentSpells {

        @Test
        void sorcerySpell() {
            var player = mock(Player.class);

            var card = Card.builder()
                    .owner(player)
                    .controller(player)
                    .name("Divination")
                    .color(Color.BLUE)
                    .type(Type.SORCERY)
                    .build();

            var spell = Spell.fromCard(card, player).build();

            assertThat(spell).isSorcery().isBlue();
        }

        @Test
        void instantSpellHasNoLoyalty() {
            var player = mock(Player.class);

            var card = Card.builder()
                    .owner(player)
                    .controller(player)
                    .name("Instant")
                    .type(Type.INSTANT)
                    .build();

            var spell = Spell.fromCard(card, player).build();

            assertThat(spell.loyalty()).isNull();
        }
    }

    @Nested
    class StackObjectInterface {

        @Test
        void spellIsStackObject() {
            var player = mock(Player.class);

            var card = Card.builder()
                    .owner(player)
                    .controller(player)
                    .name("Spell")
                    .type(Type.INSTANT)
                    .build();

            var spell = Spell.fromCard(card, player).build();

            assertThat(spell).isInstanceOf(StackObject.class);
        }
    }
}
