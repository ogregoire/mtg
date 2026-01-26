package be.imgn.mtg.engine.ability.internal.parser.selector;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.characteristics.Color;
import be.imgn.mtg.engine.characteristics.Supertype;
import be.imgn.mtg.engine.characteristics.Type;
import be.imgn.mtg.engine.characteristics.Value;
import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.mana.ManaCost;
import be.imgn.mtg.engine.object.Card;
import be.imgn.mtg.engine.object.Permanent;
import be.imgn.mtg.engine.object.Spell;
import be.imgn.mtg.engine.object.Token;

@DisplayName("ObjectSelector.matches")
class ObjectSelectorMatchesTest {

    private Player player;

    @BeforeEach
    void setUp() {
        player = mock(Player.class);
    }

    private ObjectSelector selector(TypeMatcher typeMatcher) {
        return new ObjectSelector(
                new Quantifier.One(), List.of(new Qualifier.Target()), typeMatcher, List.of(), Optional.empty());
    }

    private ObjectSelector selector(TypeMatcher typeMatcher, List<Qualifier> qualifiers) {
        return new ObjectSelector(new Quantifier.One(), qualifiers, typeMatcher, List.of(), Optional.empty());
    }

    private ObjectSelector selector(TypeMatcher typeMatcher, WithClause... clauses) {
        return new ObjectSelector(new Quantifier.One(), List.of(), typeMatcher, List.of(clauses), Optional.empty());
    }

    @Nested
    @DisplayName("Game object type filtering")
    class GameObjectTypeFiltering {

        @Test
        @DisplayName("'target creature' matches a creature permanent")
        void targetCreatureMatchesPermanent() {
            var selector = selector(new TypeMatcher.Single(Type.CREATURE));
            var permanent = Permanent.fromCard(
                            Card.builder()
                                    .owner(player)
                                    .controller(player)
                                    .name("Grizzly Bears")
                                    .type(Type.CREATURE)
                                    .build(),
                            player)
                    .build();

            assertThat(selector.matches(permanent)).isTrue();
        }

        @Test
        @DisplayName("'target creature' does not match a creature card")
        void targetCreatureDoesNotMatchCard() {
            var selector = selector(new TypeMatcher.Single(Type.CREATURE));
            var card = Card.builder()
                    .owner(player)
                    .controller(player)
                    .name("Grizzly Bears")
                    .type(Type.CREATURE)
                    .build();

            assertThat(selector.matches(card)).isFalse();
        }

        @Test
        @DisplayName("'target creature' does not match a creature spell")
        void targetCreatureDoesNotMatchSpell() {
            var selector = selector(new TypeMatcher.Single(Type.CREATURE));
            var spell = Spell.fromCard(
                            Card.builder()
                                    .owner(player)
                                    .controller(player)
                                    .name("Grizzly Bears")
                                    .type(Type.CREATURE)
                                    .build(),
                            player)
                    .build();

            assertThat(selector.matches(spell)).isFalse();
        }

        @Test
        @DisplayName("'target creature' does not match an artifact permanent")
        void targetCreatureDoesNotMatchArtifact() {
            var selector = selector(new TypeMatcher.Single(Type.CREATURE));
            var permanent = Permanent.fromCard(
                            Card.builder()
                                    .owner(player)
                                    .controller(player)
                                    .name("Sol Ring")
                                    .type(Type.ARTIFACT)
                                    .build(),
                            player)
                    .build();

            assertThat(selector.matches(permanent)).isFalse();
        }
    }

    @Nested
    @DisplayName("Negation qualifiers")
    class NegationQualifiers {

        @Test
        @DisplayName("'nonland permanent' matches a creature permanent")
        void nonlandMatchesCreature() {
            var selector =
                    selector(new TypeMatcher.Permanent(), List.of(new Qualifier.Not(new Trait.CardType(Type.LAND))));
            var permanent = Permanent.fromCard(
                            Card.builder()
                                    .owner(player)
                                    .controller(player)
                                    .name("Bear")
                                    .type(Type.CREATURE)
                                    .build(),
                            player)
                    .build();

            assertThat(selector.matches(permanent)).isTrue();
        }

        @Test
        @DisplayName("'nonland permanent' does not match a land permanent")
        void nonlandDoesNotMatchLand() {
            var selector =
                    selector(new TypeMatcher.Permanent(), List.of(new Qualifier.Not(new Trait.CardType(Type.LAND))));
            var permanent = Permanent.fromCard(
                            Card.builder()
                                    .owner(player)
                                    .controller(player)
                                    .name("Forest")
                                    .type(Type.LAND)
                                    .build(),
                            player)
                    .build();

            assertThat(selector.matches(permanent)).isFalse();
        }

        @Test
        @DisplayName("'nonblack creature' does not match a black creature")
        void nonblackDoesNotMatchBlackCreature() {
            var selector = selector(
                    new TypeMatcher.Single(Type.CREATURE),
                    List.of(new Qualifier.Not(new Trait.ObjectColor(Color.BLACK))));
            var permanent = Permanent.fromCard(
                            Card.builder()
                                    .owner(player)
                                    .controller(player)
                                    .name("Black Knight")
                                    .type(Type.CREATURE)
                                    .color(Color.BLACK)
                                    .build(),
                            player)
                    .build();

            assertThat(selector.matches(permanent)).isFalse();
        }

        @Test
        @DisplayName("'nontoken creature' matches a card-based permanent")
        void nontokenMatchesCardPermanent() {
            var selector = selector(
                    new TypeMatcher.Single(Type.CREATURE), List.of(new Qualifier.Not(new Trait.TokenSource())));
            var permanent = Permanent.fromCard(
                            Card.builder()
                                    .owner(player)
                                    .controller(player)
                                    .name("Bear")
                                    .type(Type.CREATURE)
                                    .build(),
                            player)
                    .build();

            assertThat(selector.matches(permanent)).isTrue();
        }

        @Test
        @DisplayName("'nontoken creature' does not match a token permanent")
        void nontokenDoesNotMatchTokenPermanent() {
            var selector = selector(
                    new TypeMatcher.Single(Type.CREATURE), List.of(new Qualifier.Not(new Trait.TokenSource())));
            var token = Token.builder()
                    .owner(player)
                    .controller(player)
                    .name("Soldier")
                    .type(Type.CREATURE)
                    .build();
            var permanent = Permanent.fromToken(token, player).build();

            assertThat(selector.matches(permanent)).isFalse();
        }
    }

    @Nested
    @DisplayName("Supertype qualifiers")
    class SupertypeQualifiers {

        @Test
        @DisplayName("'legendary creature' matches a legendary creature")
        void legendaryMatchesLegendary() {
            var selector = selector(
                    new TypeMatcher.Single(Type.CREATURE),
                    List.of(new Qualifier.Has(new Trait.ObjectSupertype(Supertype.LEGENDARY))));
            var permanent = Permanent.fromCard(
                            Card.builder()
                                    .owner(player)
                                    .controller(player)
                                    .name("Ragavan")
                                    .type(Type.CREATURE)
                                    .supertype(Supertype.LEGENDARY)
                                    .build(),
                            player)
                    .build();

            assertThat(selector.matches(permanent)).isTrue();
        }

        @Test
        @DisplayName("'legendary creature' does not match a non-legendary creature")
        void legendaryDoesNotMatchNonLegendary() {
            var selector = selector(
                    new TypeMatcher.Single(Type.CREATURE),
                    List.of(new Qualifier.Has(new Trait.ObjectSupertype(Supertype.LEGENDARY))));
            var permanent = Permanent.fromCard(
                            Card.builder()
                                    .owner(player)
                                    .controller(player)
                                    .name("Bear")
                                    .type(Type.CREATURE)
                                    .build(),
                            player)
                    .build();

            assertThat(selector.matches(permanent)).isFalse();
        }
    }

    @Nested
    @DisplayName("With clauses")
    class WithClauses {

        @Test
        @DisplayName("'with mana value 3 or less' matches mana value 2")
        void manaValueLessOrEqual() {
            var selector = selector(new TypeMatcher.Permanent(), new WithClause.ManaValue(Comparison.LESS_OR_EQUAL, 3));
            var permanent = Permanent.fromCard(
                            Card.builder()
                                    .owner(player)
                                    .controller(player)
                                    .name("Bear")
                                    .type(Type.CREATURE)
                                    .build(),
                            player)
                    .build();

            assertThat(selector.matches(permanent)).isTrue();
        }

        @Test
        @DisplayName("'with mana value 3' matches exactly mana value 3")
        void manaValueEqual() {
            var selector = selector(new TypeMatcher.Permanent(), new WithClause.ManaValue(Comparison.EQUAL, 3));
            var permanent = Permanent.fromCard(
                            Card.builder()
                                    .owner(player)
                                    .controller(player)
                                    .name("Divination")
                                    .type(Type.SORCERY)
                                    .manaCost(ManaCost.parse("{2}{U}"))
                                    .build(),
                            player)
                    .build();

            assertThat(selector.matches(permanent)).isTrue();
        }

        @Test
        @DisplayName("'with mana value less than 3' matches mana value 2")
        void manaValueLess() {
            var selector = selector(new TypeMatcher.Permanent(), new WithClause.ManaValue(Comparison.LESS, 3));
            var permanent = Permanent.fromCard(
                            Card.builder()
                                    .owner(player)
                                    .controller(player)
                                    .name("Bear")
                                    .type(Type.CREATURE)
                                    .build(),
                            player)
                    .build();

            assertThat(selector.matches(permanent)).isTrue();
        }

        @Test
        @DisplayName("'with mana value greater than 3' matches mana value 4")
        void manaValueGreater() {
            var selector = selector(new TypeMatcher.Permanent(), new WithClause.ManaValue(Comparison.GREATER, 3));
            var permanent = Permanent.fromCard(
                            Card.builder()
                                    .owner(player)
                                    .controller(player)
                                    .name("Big Spell")
                                    .type(Type.CREATURE)
                                    .manaCost(ManaCost.parse("{3}{G}"))
                                    .build(),
                            player)
                    .build();

            assertThat(selector.matches(permanent)).isTrue();
        }

        @Test
        @DisplayName("'with mana value 3 or greater' matches mana value 3")
        void manaValueGreaterOrEqual() {
            var selector =
                    selector(new TypeMatcher.Permanent(), new WithClause.ManaValue(Comparison.GREATER_OR_EQUAL, 3));
            var permanent = Permanent.fromCard(
                            Card.builder()
                                    .owner(player)
                                    .controller(player)
                                    .name("Divination")
                                    .type(Type.SORCERY)
                                    .manaCost(ManaCost.parse("{2}{U}"))
                                    .build(),
                            player)
                    .build();

            assertThat(selector.matches(permanent)).isTrue();
        }

        @Test
        @DisplayName("'with power 2 or less' matches power 2")
        void powerLessOrEqual() {
            var selector =
                    selector(new TypeMatcher.Single(Type.CREATURE), new WithClause.Power(Comparison.LESS_OR_EQUAL, 2));
            var permanent = Permanent.fromCard(
                            Card.builder()
                                    .owner(player)
                                    .controller(player)
                                    .name("Bear")
                                    .type(Type.CREATURE)
                                    .power(Value.of(2))
                                    .toughness(Value.of(2))
                                    .build(),
                            player)
                    .build();

            assertThat(selector.matches(permanent)).isTrue();
        }

        @Test
        @DisplayName("'with power 2 or less' does not match power 3")
        void powerTooHigh() {
            var selector =
                    selector(new TypeMatcher.Single(Type.CREATURE), new WithClause.Power(Comparison.LESS_OR_EQUAL, 2));
            var permanent = Permanent.fromCard(
                            Card.builder()
                                    .owner(player)
                                    .controller(player)
                                    .name("Giant")
                                    .type(Type.CREATURE)
                                    .power(Value.of(3))
                                    .toughness(Value.of(3))
                                    .build(),
                            player)
                    .build();

            assertThat(selector.matches(permanent)).isFalse();
        }

        @Test
        @DisplayName("'with power 2 or less' does not match non-creature without power")
        void powerNullForNonCreature() {
            var selector = selector(new TypeMatcher.Permanent(), new WithClause.Power(Comparison.LESS_OR_EQUAL, 2));
            var permanent = Permanent.fromCard(
                            Card.builder()
                                    .owner(player)
                                    .controller(player)
                                    .name("Sol Ring")
                                    .type(Type.ARTIFACT)
                                    .build(),
                            player)
                    .build();

            assertThat(selector.matches(permanent)).isFalse();
        }

        @Test
        @DisplayName("'with toughness 3 or less' matches toughness 2")
        void toughnessLessOrEqual() {
            var selector = selector(
                    new TypeMatcher.Single(Type.CREATURE), new WithClause.Toughness(Comparison.LESS_OR_EQUAL, 3));
            var permanent = Permanent.fromCard(
                            Card.builder()
                                    .owner(player)
                                    .controller(player)
                                    .name("Bear")
                                    .type(Type.CREATURE)
                                    .power(Value.of(2))
                                    .toughness(Value.of(2))
                                    .build(),
                            player)
                    .build();

            assertThat(selector.matches(permanent)).isTrue();
        }

        @Test
        @DisplayName("'with toughness 2 or less' does not match toughness 3")
        void toughnessTooHigh() {
            var selector = selector(
                    new TypeMatcher.Single(Type.CREATURE), new WithClause.Toughness(Comparison.LESS_OR_EQUAL, 2));
            var permanent = Permanent.fromCard(
                            Card.builder()
                                    .owner(player)
                                    .controller(player)
                                    .name("Giant")
                                    .type(Type.CREATURE)
                                    .power(Value.of(3))
                                    .toughness(Value.of(3))
                                    .build(),
                            player)
                    .build();

            assertThat(selector.matches(permanent)).isFalse();
        }

        @Test
        @DisplayName("'with toughness 2 or less' does not match non-creature without toughness")
        void toughnessNullForNonCreature() {
            var selector = selector(new TypeMatcher.Permanent(), new WithClause.Toughness(Comparison.LESS_OR_EQUAL, 2));
            var permanent = Permanent.fromCard(
                            Card.builder()
                                    .owner(player)
                                    .controller(player)
                                    .name("Sol Ring")
                                    .type(Type.ARTIFACT)
                                    .build(),
                            player)
                    .build();

            assertThat(selector.matches(permanent)).isFalse();
        }
    }

    @Nested
    @DisplayName("Status qualifiers")
    class StatusQualifiers {

        @Test
        @DisplayName("tapped status matches tapped permanent")
        void tappedMatchesTapped() {
            var selector = selector(new TypeMatcher.Permanent(), List.of(new Qualifier.Status(StatusType.TAPPED)));
            var permanent = Permanent.fromCard(
                            Card.builder()
                                    .owner(player)
                                    .controller(player)
                                    .name("Forest")
                                    .type(Type.LAND)
                                    .build(),
                            player)
                    .build();
            permanent.tap();

            assertThat(selector.matches(permanent)).isTrue();
        }

        @Test
        @DisplayName("tapped status does not match untapped permanent")
        void tappedDoesNotMatchUntapped() {
            var selector = selector(new TypeMatcher.Permanent(), List.of(new Qualifier.Status(StatusType.TAPPED)));
            var permanent = Permanent.fromCard(
                            Card.builder()
                                    .owner(player)
                                    .controller(player)
                                    .name("Forest")
                                    .type(Type.LAND)
                                    .build(),
                            player)
                    .build();

            assertThat(selector.matches(permanent)).isFalse();
        }

        @Test
        @DisplayName("untapped status matches untapped permanent")
        void untappedMatchesUntapped() {
            var selector = selector(new TypeMatcher.Permanent(), List.of(new Qualifier.Status(StatusType.UNTAPPED)));
            var permanent = Permanent.fromCard(
                            Card.builder()
                                    .owner(player)
                                    .controller(player)
                                    .name("Forest")
                                    .type(Type.LAND)
                                    .build(),
                            player)
                    .build();

            assertThat(selector.matches(permanent)).isTrue();
        }

        @Test
        @DisplayName("untapped status does not match tapped permanent")
        void untappedDoesNotMatchTapped() {
            var selector = selector(new TypeMatcher.Permanent(), List.of(new Qualifier.Status(StatusType.UNTAPPED)));
            var permanent = Permanent.fromCard(
                            Card.builder()
                                    .owner(player)
                                    .controller(player)
                                    .name("Forest")
                                    .type(Type.LAND)
                                    .build(),
                            player)
                    .build();
            permanent.tap();

            assertThat(selector.matches(permanent)).isFalse();
        }

        @Test
        @DisplayName("status does not match non-permanent")
        void statusDoesNotMatchCard() {
            var selector = selector(new TypeMatcher.Card(), List.of(new Qualifier.Status(StatusType.TAPPED)));
            var card = Card.builder()
                    .owner(player)
                    .controller(player)
                    .name("Forest")
                    .type(Type.LAND)
                    .build();

            assertThat(selector.matches(card)).isFalse();
        }
    }
}
