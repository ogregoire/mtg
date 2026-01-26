package be.imgn.mtg.engine.object;

import static be.imgn.mtg.engine.assertions.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.ability.StaticAbility;
import be.imgn.mtg.engine.characteristics.Color;
import be.imgn.mtg.engine.characteristics.Colors;
import be.imgn.mtg.engine.characteristics.CreatureType;
import be.imgn.mtg.engine.characteristics.PlaneswalkerType;
import be.imgn.mtg.engine.characteristics.Supertype;
import be.imgn.mtg.engine.characteristics.Type;
import be.imgn.mtg.engine.characteristics.Value;
import be.imgn.mtg.engine.cost.internal.TapCost;
import be.imgn.mtg.engine.cost.internal.UntapCost;
import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.mana.ManaCost;

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

    @Nested
    class ColorIndicator {

        @Test
        void cardWithColorIndicator() {
            var player = mock(Player.class);

            var card = Card.builder()
                    .owner(player)
                    .controller(player)
                    .name("Pact of Negation")
                    .type(Type.INSTANT)
                    .colorIndicator(Colors.of(Color.BLUE))
                    .color(Color.BLUE)
                    .build();

            assertThat(card.colorIndicator()).isBlue();
            assertThat(card.colors()).isBlue();
        }

        @Test
        void addColorIndicator() {
            var player = mock(Player.class);

            var card = Card.builder()
                    .owner(player)
                    .controller(player)
                    .name("Test Card")
                    .type(Type.CREATURE)
                    .addColorIndicator(Color.RED)
                    .addColorIndicator(Color.GREEN)
                    .build();

            assertThat(card.colorIndicator()).isRed().isGreen();
        }

        @Test
        void emptyColorIndicatorByDefault() {
            var player = mock(Player.class);

            var card = Card.builder()
                    .owner(player)
                    .controller(player)
                    .name("Test")
                    .type(Type.CREATURE)
                    .build();

            assertThat(card.colorIndicator()).isEmpty();
        }
    }

    @Nested
    class ManaCostHandling {

        @Test
        void cardWithManaCost() {
            var player = mock(Player.class);
            var manaCost = mock(ManaCost.class);

            var card = Card.builder()
                    .owner(player)
                    .controller(player)
                    .name("Test Card")
                    .type(Type.CREATURE)
                    .manaCost(manaCost)
                    .build();

            assertThat(card).hasManaCost();
            assertThat(card.manaCost()).isSameAs(manaCost);
        }

        @Test
        void cardWithoutManaCost() {
            var player = mock(Player.class);

            var card = Card.builder()
                    .owner(player)
                    .controller(player)
                    .name("Land")
                    .type(Type.LAND)
                    .build();

            assertThat(card).hasNoManaCost();
            assertThat(card.manaCost()).isNull();
        }
    }

    @Nested
    class ObjectId {

        @Test
        void eachCardGetsUniqueId() {
            var player = mock(Player.class);

            var card1 = Card.builder()
                    .owner(player)
                    .controller(player)
                    .name("Card 1")
                    .type(Type.CREATURE)
                    .build();

            var card2 = Card.builder()
                    .owner(player)
                    .controller(player)
                    .name("Card 2")
                    .type(Type.CREATURE)
                    .build();

            assertThat(card1.id()).isNotNull();
            assertThat(card2.id()).isNotNull();
            assertThat(card1.id()).isNotEqualTo(card2.id());
        }

        @Test
        void sameCardKeepsIdBetweenReferences() {
            var player = mock(Player.class);

            var card = Card.builder()
                    .owner(player)
                    .controller(player)
                    .name("Test")
                    .type(Type.CREATURE)
                    .build();

            var id1 = card.id();
            var id2 = card.id();

            assertThat(id1).isSameAs(id2);
        }
    }

    @Nested
    class Abilities {

        @Test
        void cardWithAbility() {
            var player = mock(Player.class);
            var ability = mock(StaticAbility.class);

            var card = Card.builder()
                    .owner(player)
                    .controller(player)
                    .name("Test Card")
                    .type(Type.CREATURE)
                    .ability(ability)
                    .build();

            assertThat(card.abilities()).hasCount(1).contains(ability);
        }

        @Test
        void cardWithMultipleAbilities() {
            var player = mock(Player.class);
            var ability1 = mock(StaticAbility.class);
            var ability2 = mock(StaticAbility.class);

            var card = Card.builder()
                    .owner(player)
                    .controller(player)
                    .name("Test Card")
                    .type(Type.CREATURE)
                    .addAbility(ability1)
                    .addAbility(ability2)
                    .build();

            assertThat(card.abilities()).hasCount(2).contains(ability1).contains(ability2);
        }

        @Test
        void noAbilitiesByDefault() {
            var player = mock(Player.class);

            var card = Card.builder()
                    .owner(player)
                    .controller(player)
                    .name("Test")
                    .type(Type.CREATURE)
                    .build();

            assertThat(card.abilities()).isEmpty();
        }
    }

    @Nested
    class CreatureCards {

        @Test
        void creatureWithPowerAndToughness() {
            var player = mock(Player.class);

            var card = Card.builder()
                    .owner(player)
                    .controller(player)
                    .name("Creature")
                    .type(Type.CREATURE)
                    .power(Value.of(3))
                    .toughness(Value.of(4))
                    .build();

            assertThat(card.power()).isEqualTo(Value.of(3));
            assertThat(card.toughness()).isEqualTo(Value.of(4));
        }

        @Test
        void nonCreatureHasNoPowerToughness() {
            var player = mock(Player.class);

            var card = Card.builder()
                    .owner(player)
                    .controller(player)
                    .name("Instant")
                    .type(Type.INSTANT)
                    .build();

            assertThat(card.power()).isNull();
            assertThat(card.toughness()).isNull();
        }
    }

    @Nested
    class PlaneswalkerCards {

        @Test
        void planeswalkerWithLoyalty() {
            var player = mock(Player.class);

            var card = Card.builder()
                    .owner(player)
                    .controller(player)
                    .name("Jace")
                    .color(Color.BLUE)
                    .type(Type.PLANESWALKER)
                    .subtype(PlaneswalkerType.JACE)
                    .loyalty(Value.of(4))
                    .build();

            assertThat(card.loyalty()).isEqualTo(Value.of(4));
        }

        @Test
        void nonPlaneswalkerHasNoLoyalty() {
            var player = mock(Player.class);

            var card = Card.builder()
                    .owner(player)
                    .controller(player)
                    .name("Creature")
                    .type(Type.CREATURE)
                    .build();

            assertThat(card.loyalty()).isNull();
        }
    }

    @Nested
    class BuilderPatterns {

        @Test
        void builderSupportsMethodChaining() {
            var player = mock(Player.class);

            var card = Card.builder()
                    .owner(player)
                    .controller(player)
                    .name("Chained")
                    .type(Type.CREATURE)
                    .color(Color.WHITE)
                    .power(Value.of(1))
                    .toughness(Value.of(1))
                    .build();

            assertThat(card.name()).isEqualTo("Chained");
        }

        @Test
        void ownerAndControllerCanBeDifferent() {
            var owner = mock(Player.class);
            var controller = mock(Player.class);

            var card = Card.builder()
                    .owner(owner)
                    .controller(controller)
                    .name("Stolen Card")
                    .type(Type.CREATURE)
                    .build();

            assertThat(card.owner()).isSameAs(owner);
            assertThat(card.controller()).isSameAs(controller);
        }
    }

    @Nested
    class Costs {

        @Test
        void cardWithCost() {
            var player = mock(Player.class);
            var cost = new TapCost();

            var card = Card.builder()
                    .owner(player)
                    .controller(player)
                    .name("Test Card")
                    .type(Type.CREATURE)
                    .cost(cost)
                    .build();

            assertThat(card.costs()).hasCount(1).contains(cost);
        }

        @Test
        void cardWithMultipleCosts() {
            var player = mock(Player.class);
            var cost1 = new TapCost();
            var cost2 = new UntapCost();

            var card = Card.builder()
                    .owner(player)
                    .controller(player)
                    .name("Test Card")
                    .type(Type.CREATURE)
                    .addCost(cost1)
                    .addCost(cost2)
                    .build();

            assertThat(card.costs()).hasCount(2).contains(cost1).contains(cost2);
        }
    }
}
