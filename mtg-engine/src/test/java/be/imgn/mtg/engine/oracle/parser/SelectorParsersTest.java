package be.imgn.mtg.engine.oracle.parser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import com.google.mu.util.CharPredicate;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.oracle.domain.*;

class SelectorParsersTest {

    private static final CharPredicate SPACE = CharPredicate.is(' ');

    // ── Color ──────────────────────────────────────────────────────────────

    @Nested
    class ColorParser {

        @Test
        void parsesWhite() {
            var result = SelectorParsers.COLOR.parseSkipping(SPACE, "white");
            assertThat(result).isEqualTo(Color.WHITE);
        }

        @Test
        void parsesWhiteTitleCase() {
            var result = SelectorParsers.COLOR.parseSkipping(SPACE, "White");
            assertThat(result).isEqualTo(Color.WHITE);
        }

        @Test
        void parsesBlue() {
            var result = SelectorParsers.COLOR.parseSkipping(SPACE, "blue");
            assertThat(result).isEqualTo(Color.BLUE);
        }

        @Test
        void parsesBlack() {
            var result = SelectorParsers.COLOR.parseSkipping(SPACE, "black");
            assertThat(result).isEqualTo(Color.BLACK);
        }

        @Test
        void parsesRed() {
            var result = SelectorParsers.COLOR.parseSkipping(SPACE, "red");
            assertThat(result).isEqualTo(Color.RED);
        }

        @Test
        void parsesGreen() {
            var result = SelectorParsers.COLOR.parseSkipping(SPACE, "green");
            assertThat(result).isEqualTo(Color.GREEN);
        }
    }

    // ── CardType ──────────────────────────────────────────────────────────

    @Nested
    class CardTypeParser {

        @Test
        void parsesCreature() {
            var result = SelectorParsers.CARD_TYPE.parseSkipping(SPACE, "creature");
            assertThat(result).isEqualTo(CardType.CREATURE);
        }

        @Test
        void parsesArtifact() {
            var result = SelectorParsers.CARD_TYPE.parseSkipping(SPACE, "artifact");
            assertThat(result).isEqualTo(CardType.ARTIFACT);
        }

        @Test
        void parsesEnchantment() {
            var result = SelectorParsers.CARD_TYPE.parseSkipping(SPACE, "enchantment");
            assertThat(result).isEqualTo(CardType.ENCHANTMENT);
        }

        @Test
        void parsesLand() {
            var result = SelectorParsers.CARD_TYPE.parseSkipping(SPACE, "land");
            assertThat(result).isEqualTo(CardType.LAND);
        }

        @Test
        void parsesPlaneswalker() {
            var result = SelectorParsers.CARD_TYPE.parseSkipping(SPACE, "planeswalker");
            assertThat(result).isEqualTo(CardType.PLANESWALKER);
        }

        @Test
        void parsesInstant() {
            var result = SelectorParsers.CARD_TYPE.parseSkipping(SPACE, "instant");
            assertThat(result).isEqualTo(CardType.INSTANT);
        }

        @Test
        void parsesSorcery() {
            var result = SelectorParsers.CARD_TYPE.parseSkipping(SPACE, "sorcery");
            assertThat(result).isEqualTo(CardType.SORCERY);
        }
    }

    // ── GameObjectType ────────────────────────────────────────────────────

    @Nested
    class GameObjectTypeParser {

        @Test
        void parsesPermanent() {
            var result = SelectorParsers.GAME_OBJECT_TYPE.parseSkipping(SPACE, "permanent");
            assertThat(result).isEqualTo(GameObjectType.PERMANENT);
        }

        @Test
        void parsesSpell() {
            var result = SelectorParsers.GAME_OBJECT_TYPE.parseSkipping(SPACE, "spell");
            assertThat(result).isEqualTo(GameObjectType.SPELL);
        }

        @Test
        void parsesCard() {
            var result = SelectorParsers.GAME_OBJECT_TYPE.parseSkipping(SPACE, "card");
            assertThat(result).isEqualTo(GameObjectType.CARD);
        }
    }

    // ── WORD_NUMBER ───────────────────────────────────────────────────────

    @Nested
    class WordNumber {

        @Test
        void parsesOne() {
            var result = SelectorParsers.WORD_NUMBER.parseSkipping(SPACE, "one");
            assertThat(result).isEqualTo(1);
        }

        @Test
        void parsesTwo() {
            var result = SelectorParsers.WORD_NUMBER.parseSkipping(SPACE, "two");
            assertThat(result).isEqualTo(2);
        }

        @Test
        void parsesThree() {
            var result = SelectorParsers.WORD_NUMBER.parseSkipping(SPACE, "three");
            assertThat(result).isEqualTo(3);
        }

        @Test
        void parsesTen() {
            var result = SelectorParsers.WORD_NUMBER.parseSkipping(SPACE, "ten");
            assertThat(result).isEqualTo(10);
        }

        @Test
        void parsesTwenty() {
            var result = SelectorParsers.WORD_NUMBER.parseSkipping(SPACE, "twenty");
            assertThat(result).isEqualTo(20);
        }

        @Test
        void parsesTitleCase() {
            var result = SelectorParsers.WORD_NUMBER.parseSkipping(SPACE, "Three");
            assertThat(result).isEqualTo(3);
        }
    }

    // ── INTEGER ───────────────────────────────────────────────────────────

    @Nested
    class IntegerParser {

        @Test
        void parsesSingleDigit() {
            var result = SelectorParsers.INTEGER.parseSkipping(SPACE, "3");
            assertThat(result).isEqualTo(3);
        }

        @Test
        void parsesMultiDigit() {
            var result = SelectorParsers.INTEGER.parseSkipping(SPACE, "12");
            assertThat(result).isEqualTo(12);
        }

        @Test
        void parsesZero() {
            var result = SelectorParsers.INTEGER.parseSkipping(SPACE, "0");
            assertThat(result).isEqualTo(0);
        }
    }

    // ── Amount ────────────────────────────────────────────────────────────

    @Nested
    class AmountParser {

        @Test
        void parsesExactInteger() {
            var result = SelectorParsers.AMOUNT.parseSkipping(SPACE, "3");
            assertThat(result).isEqualTo(new Amount.Exact(3));
        }

        @Test
        void parsesExactWordNumber() {
            var result = SelectorParsers.AMOUNT.parseSkipping(SPACE, "two");
            assertThat(result).isEqualTo(new Amount.Exact(2));
        }

        @Test
        void parsesVariableX() {
            var result = SelectorParsers.AMOUNT.parseSkipping(SPACE, "X");
            assertThat(result).isInstanceOf(Amount.Variable.class);
        }

        @Test
        void parsesThatMuch() {
            var result = SelectorParsers.AMOUNT.parseSkipping(SPACE, "that much");
            assertThat(result).isEqualTo(new Amount.Reference("that much"));
        }

        @Test
        void parsesThatMany() {
            var result = SelectorParsers.AMOUNT.parseSkipping(SPACE, "that many");
            assertThat(result).isEqualTo(new Amount.Reference("that many"));
        }
    }

    // ── CounterType ───────────────────────────────────────────────────────

    @Nested
    class CounterTypeParser {

        @Test
        void parsesPlusPlusCounter() {
            var result = SelectorParsers.COUNTER_TYPE.parseSkipping(SPACE, "+1/+1");
            assertThat(result).isEqualTo(new CounterType.PtCounter(1, 1));
        }

        @Test
        void parsesMinusMinusCounter() {
            var result = SelectorParsers.COUNTER_TYPE.parseSkipping(SPACE, "-1/-1");
            assertThat(result).isEqualTo(new CounterType.PtCounter(-1, -1));
        }

        @Test
        void parsesStandardCounter() {
            var result = SelectorParsers.COUNTER_TYPE.parseSkipping(SPACE, "loyalty");
            assertThat(result).isEqualTo(CounterType.Named.LOYALTY);
        }

        @Test
        void parsesKeywordCounter() {
            var result = SelectorParsers.COUNTER_TYPE.parseSkipping(SPACE, "first strike");
            assertThat(result).isEqualTo(CounterType.Keyword.FIRST_STRIKE);
            assertThat(((CounterType.Keyword) result).ability()).isEqualTo(Ability.StaticKeyword.FIRST_STRIKE);
        }

        @Test
        void parsesTriggeredKeywordCounter() {
            var result = SelectorParsers.COUNTER_TYPE.parseSkipping(SPACE, "exalted");
            assertThat(result).isEqualTo(CounterType.Keyword.EXALTED);
            assertThat(((CounterType.Keyword) result).ability()).isEqualTo(Ability.TriggeredKeyword.EXALTED);
        }

        @Test
        void parsesMechanicSpecificStandardCounter() {
            var result = SelectorParsers.COUNTER_TYPE.parseSkipping(SPACE, "charge");
            assertThat(result).isEqualTo(CounterType.Named.CHARGE);
        }

        @Test
        void rejectsUnknownCounterName() {
            assertThatThrownBy(() -> SelectorParsers.COUNTER_TYPE.parseSkipping(SPACE, "nonesuch"))
                    .isInstanceOf(Exception.class);
        }
    }

    // ── PtValue ───────────────────────────────────────────────────────────

    @Nested
    class PtValueParser {

        @Test
        void parsesTwoThree() {
            var result = SelectorParsers.PT_VALUE.parseSkipping(SPACE, "2/3");
            assertThat(result).isEqualTo(new PtValue(2, 3));
        }

        @Test
        void parsesOneOne() {
            var result = SelectorParsers.PT_VALUE.parseSkipping(SPACE, "1/1");
            assertThat(result).isEqualTo(new PtValue(1, 1));
        }

        @Test
        void parsesZeroZero() {
            var result = SelectorParsers.PT_VALUE.parseSkipping(SPACE, "0/0");
            assertThat(result).isEqualTo(new PtValue(0, 0));
        }
    }

    // ── Quantifier ────────────────────────────────────────────────────────

    @Nested
    class QuantifierParser {

        @Test
        void parsesA() {
            var result = SelectorParsers.QUANTIFIER.parseSkipping(SPACE, "a");
            assertThat(result).isInstanceOf(Selector.Quantifier.One.class);
        }

        @Test
        void parsesAn() {
            var result = SelectorParsers.QUANTIFIER.parseSkipping(SPACE, "an");
            assertThat(result).isInstanceOf(Selector.Quantifier.One.class);
        }

        @Test
        void parsesAll() {
            var result = SelectorParsers.QUANTIFIER.parseSkipping(SPACE, "all");
            assertThat(result).isInstanceOf(Selector.Quantifier.All.class);
        }

        @Test
        void parsesEach() {
            var result = SelectorParsers.QUANTIFIER.parseSkipping(SPACE, "each");
            assertThat(result).isInstanceOf(Selector.Quantifier.Each.class);
        }

        @Test
        void parsesAnother() {
            var result = SelectorParsers.QUANTIFIER.parseSkipping(SPACE, "another");
            assertThat(result).isInstanceOf(Selector.Quantifier.Another.class);
        }

        @Test
        void parsesUpToTwo() {
            var result = SelectorParsers.QUANTIFIER.parseSkipping(SPACE, "up to two");
            assertThat(result).isEqualTo(new Selector.Quantifier.UpTo(2));
        }

        @Test
        void parsesUpToThree() {
            var result = SelectorParsers.QUANTIFIER.parseSkipping(SPACE, "up to 3");
            assertThat(result).isEqualTo(new Selector.Quantifier.UpTo(3));
        }

        @Test
        void parsesAnyNumberOf() {
            var result = SelectorParsers.QUANTIFIER.parseSkipping(SPACE, "any number of");
            assertThat(result).isInstanceOf(Selector.Quantifier.AnyNumber.class);
        }

        @Test
        void parsesVariableX() {
            var result = SelectorParsers.QUANTIFIER.parseSkipping(SPACE, "X");
            assertThat(result).isInstanceOf(Selector.Quantifier.Variable.class);
        }

        @Test
        void parsesWordCount() {
            var result = SelectorParsers.QUANTIFIER.parseSkipping(SPACE, "two");
            assertThat(result).isEqualTo(new Selector.Quantifier.Count(2));
        }

        @Test
        void parsesIntegerCount() {
            var result = SelectorParsers.QUANTIFIER.parseSkipping(SPACE, "3");
            assertThat(result).isEqualTo(new Selector.Quantifier.Count(3));
        }
    }

    // ── TypeExpression ────────────────────────────────────────────────────

    @Nested
    class TypeExpressionParser {

        @Test
        void parsesSingleCreature() {
            var result = SelectorParsers.TYPE_EXPRESSION.parseSkipping(SPACE, "creature");
            assertThat(result)
                    .isEqualTo(new Selector.TypeExpression.Single(new Selector.SingleType.OfCard(CardType.CREATURE)));
        }

        @Test
        void parsesSinglePermanent() {
            var result = SelectorParsers.TYPE_EXPRESSION.parseSkipping(SPACE, "permanent");
            assertThat(result)
                    .isEqualTo(new Selector.TypeExpression.Single(
                            new Selector.SingleType.OfGameObject(GameObjectType.PERMANENT)));
        }

        @Test
        void parsesArtifactOrEnchantment() {
            var result = SelectorParsers.TYPE_EXPRESSION.parseSkipping(SPACE, "artifact or enchantment");
            assertThat(result).isInstanceOf(Selector.TypeExpression.Or.class);
            var or = (Selector.TypeExpression.Or) result;
            assertThat(or.alternatives()).hasSize(2);
            assertThat(or.alternatives().get(0))
                    .isEqualTo(new Selector.TypeExpression.Or.Alternative(
                            new Selector.SingleType.OfCard(CardType.ARTIFACT)));
            assertThat(or.alternatives().get(1))
                    .isEqualTo(new Selector.TypeExpression.Or.Alternative(
                            new Selector.SingleType.OfCard(CardType.ENCHANTMENT)));
        }

        @Test
        void parsesArtifactCreatureCompound() {
            var result = SelectorParsers.TYPE_EXPRESSION.parseSkipping(SPACE, "artifact creature");
            assertThat(result).isInstanceOf(Selector.TypeExpression.Compound.class);
            var compound = (Selector.TypeExpression.Compound) result;
            assertThat(compound.types()).hasSize(2);
            assertThat(compound.types().get(0)).isEqualTo(new Selector.SingleType.OfCard(CardType.ARTIFACT));
            assertThat(compound.types().get(1)).isEqualTo(new Selector.SingleType.OfCard(CardType.CREATURE));
        }

        // Regression tests for the Or-of-compound refactor — each or-list
        // item must remain parseable as a compound or a single.

        @Test
        void parsesOrOfCompoundSecond() {
            var result = SelectorParsers.TYPE_EXPRESSION.parseSkipping(SPACE, "creature or enchantment creature");
            assertThat(result).isInstanceOf(Selector.TypeExpression.Or.class);
            var or = (Selector.TypeExpression.Or) result;
            assertThat(or.alternatives()).hasSize(2);
            assertThat(or.alternatives().get(0))
                    .isEqualTo(new Selector.TypeExpression.Or.Alternative(
                            new Selector.SingleType.OfCard(CardType.CREATURE)));
            assertThat(or.alternatives().get(1))
                    .isEqualTo(new Selector.TypeExpression.Or.Alternative(new Selector.TypeExpression.Compound(List.of(
                            new Selector.SingleType.OfCard(CardType.ENCHANTMENT),
                            new Selector.SingleType.OfCard(CardType.CREATURE)))));
        }

        @Test
        void parsesOrOfCompoundFirst() {
            var result = SelectorParsers.TYPE_EXPRESSION.parseSkipping(SPACE, "artifact creature or land");
            assertThat(result).isInstanceOf(Selector.TypeExpression.Or.class);
            var or = (Selector.TypeExpression.Or) result;
            assertThat(or.alternatives()).hasSize(2);
            assertThat(or.alternatives().get(0))
                    .isEqualTo(new Selector.TypeExpression.Or.Alternative(new Selector.TypeExpression.Compound(List.of(
                            new Selector.SingleType.OfCard(CardType.ARTIFACT),
                            new Selector.SingleType.OfCard(CardType.CREATURE)))));
            assertThat(or.alternatives().get(1))
                    .isEqualTo(
                            new Selector.TypeExpression.Or.Alternative(new Selector.SingleType.OfCard(CardType.LAND)));
        }

        @Test
        void parsesOxfordOrOfThreeSingles() {
            var result = SelectorParsers.TYPE_EXPRESSION.parseSkipping(SPACE, "artifact, enchantment, or land");
            assertThat(result).isInstanceOf(Selector.TypeExpression.Or.class);
            var or = (Selector.TypeExpression.Or) result;
            assertThat(or.alternatives()).hasSize(3);
            assertThat(or.alternatives().get(0))
                    .isEqualTo(new Selector.TypeExpression.Or.Alternative(
                            new Selector.SingleType.OfCard(CardType.ARTIFACT)));
            assertThat(or.alternatives().get(1))
                    .isEqualTo(new Selector.TypeExpression.Or.Alternative(
                            new Selector.SingleType.OfCard(CardType.ENCHANTMENT)));
            assertThat(or.alternatives().get(2))
                    .isEqualTo(
                            new Selector.TypeExpression.Or.Alternative(new Selector.SingleType.OfCard(CardType.LAND)));
        }

        @Test
        void parsesAndOrCreaturesPlaneswalkers() {
            var result = SelectorParsers.TYPE_EXPRESSION.parseSkipping(SPACE, "creatures and/or planeswalkers");
            assertThat(result).isInstanceOf(Selector.TypeExpression.Or.class);
            var or = (Selector.TypeExpression.Or) result;
            assertThat(or.alternatives()).hasSize(2);
            assertThat(or.alternatives().get(0))
                    .isEqualTo(new Selector.TypeExpression.Or.Alternative(
                            new Selector.SingleType.OfCard(CardType.CREATURE)));
            assertThat(or.alternatives().get(1))
                    .isEqualTo(new Selector.TypeExpression.Or.Alternative(
                            new Selector.SingleType.OfCard(CardType.PLANESWALKER)));
        }

        @Test
        void parsesAndTypeAsOr() {
            var result = SelectorParsers.TYPE_EXPRESSION.parseSkipping(SPACE, "instant and sorcery");
            assertThat(result).isInstanceOf(Selector.TypeExpression.Or.class);
            var or = (Selector.TypeExpression.Or) result;
            assertThat(or.alternatives()).hasSize(2);
            assertThat(or.alternatives().get(0))
                    .isEqualTo(new Selector.TypeExpression.Or.Alternative(
                            new Selector.SingleType.OfCard(CardType.INSTANT)));
            assertThat(or.alternatives().get(1))
                    .isEqualTo(new Selector.TypeExpression.Or.Alternative(
                            new Selector.SingleType.OfCard(CardType.SORCERY)));
        }
    }

    // ── Selector ──────────────────────────────────────────────────────────

    @Nested
    class SelectorParser {

        @Test
        void parsesTargetCreature() {
            var result = SelectorParsers.SELECTOR.parseSkipping(SPACE, "target creature");
            assertThat(result.quantifier()).isInstanceOf(Selector.Quantifier.One.class);
            assertThat(result.objectType()).isEqualTo(GameObjectType.PERMANENT);
            assertThat(result.qualifiers())
                    .containsExactly(Selector.Qualifier.TARGET, new Selector.Qualifier.Types(TypeMatcher.CREATURE));
        }

        @Test
        void parsesAllNonlandPermanent() {
            var result = SelectorParsers.SELECTOR.parseSkipping(SPACE, "all nonland permanent");
            assertThat(result.quantifier()).isInstanceOf(Selector.Quantifier.All.class);
            assertThat(result.objectType()).isEqualTo(GameObjectType.PERMANENT);
            assertThat(result.qualifiers()).containsExactly(new Selector.Qualifier.Types(TypeMatcher.NONLAND));
        }

        @Test
        void parsesARedCreature() {
            var result = SelectorParsers.SELECTOR.parseSkipping(SPACE, "a red creature");
            assertThat(result.quantifier()).isInstanceOf(Selector.Quantifier.One.class);
            assertThat(result.objectType()).isEqualTo(GameObjectType.PERMANENT);
            assertThat(result.qualifiers())
                    .containsExactly(
                            new Selector.Qualifier.Colors(new ColorMatcher.Is(Color.RED)),
                            new Selector.Qualifier.Types(TypeMatcher.CREATURE));
        }

        @Test
        void parsesUpToTwoTargetCreature() {
            var result = SelectorParsers.SELECTOR.parseSkipping(SPACE, "up to two target creature");
            assertThat(result.quantifier()).isEqualTo(new Selector.Quantifier.UpTo(2));
            assertThat(result.qualifiers())
                    .containsExactly(Selector.Qualifier.TARGET, new Selector.Qualifier.Types(TypeMatcher.CREATURE));
        }

        @Test
        void parsesTargetTappedCreature() {
            var result = SelectorParsers.SELECTOR.parseSkipping(SPACE, "target tapped creature");
            assertThat(result.qualifiers())
                    .containsExactly(
                            Selector.Qualifier.TARGET,
                            Selector.Qualifier.Status.TAPPED,
                            new Selector.Qualifier.Types(TypeMatcher.CREATURE));
        }

        @Test
        void parsesTargetAttackingCreature() {
            var result = SelectorParsers.SELECTOR.parseSkipping(SPACE, "target attacking creature");
            assertThat(result.qualifiers())
                    .containsExactly(
                            Selector.Qualifier.TARGET,
                            new Selector.Qualifier.CombatStatus("attacking"),
                            new Selector.Qualifier.Types(TypeMatcher.CREATURE));
        }

        @Test
        void parsesTargetLegendaryCreature() {
            var result = SelectorParsers.SELECTOR.parseSkipping(SPACE, "target legendary creature");
            assertThat(result.qualifiers())
                    .containsExactly(
                            Selector.Qualifier.TARGET,
                            new Selector.Qualifier.Types(
                                    new TypeMatcher.All(List.of(TypeMatcher.LEGENDARY, TypeMatcher.CREATURE))));
        }

        @Test
        void parsesWithClausePowerThreeOrLess() {
            var result = SelectorParsers.SELECTOR.parseSkipping(SPACE, "target creature with power 3 or less");
            assertThat(result.withClauses()).hasSize(1);
            assertThat(result.withClauses().getFirst())
                    .isEqualTo(Selector.WithClause.with(new Selector.WithClause.Body.PtComparison(
                            Selector.WithClause.Body.PtComparison.Aspect.POWER,
                            Selector.WithClause.Body.PtComparison.Comparator.LESS_THAN_OR_EQUAL,
                            "3")));
        }

        @Test
        void parsesWithClauseFlying() {
            var result = SelectorParsers.SELECTOR.parseSkipping(SPACE, "target creature with flying");
            assertThat(result.withClauses()).hasSize(1);
            assertThat(result.withClauses().getFirst())
                    .isEqualTo(Selector.WithClause.with(
                            new Selector.WithClause.Body.HasAbility(Ability.StaticKeyword.FLYING)));
        }

        @Test
        void parsesControllerClauseYouControl() {
            var result = SelectorParsers.SELECTOR.parseSkipping(SPACE, "a creature you control");
            assertThat(result.controller())
                    .isEqualTo(Selector.ControllerClause.does(
                            new Selector.ControllerClause.Body.Controls(Selector.ControllerClause.Who.YOU)));
        }

        @Test
        void parsesControllerClauseOpponentControls() {
            var result = SelectorParsers.SELECTOR.parseSkipping(SPACE, "a creature an opponent controls");
            assertThat(result.controller())
                    .isEqualTo(Selector.ControllerClause.does(
                            new Selector.ControllerClause.Body.Controls(Selector.ControllerClause.Who.AN_OPPONENT)));
        }

        @Test
        void parsesTargetSpell() {
            var result = SelectorParsers.SELECTOR.parseSkipping(SPACE, "target spell");
            assertThat(result.objectType()).isEqualTo(GameObjectType.SPELL);
            assertThat(result.qualifiers()).containsExactly(Selector.Qualifier.TARGET);
        }

        @Test
        void parsesEachCreature() {
            var result = SelectorParsers.SELECTOR.parseSkipping(SPACE, "each creature");
            assertThat(result.quantifier()).isInstanceOf(Selector.Quantifier.Each.class);
            assertThat(result.objectType()).isEqualTo(GameObjectType.PERMANENT);
            assertThat(result.qualifiers()).containsExactly(new Selector.Qualifier.Types(TypeMatcher.CREATURE));
        }

        @Test
        void parsesTargetNonblackCreature() {
            var result = SelectorParsers.SELECTOR.parseSkipping(SPACE, "target nonblack creature");
            assertThat(result.qualifiers())
                    .containsExactly(
                            Selector.Qualifier.TARGET,
                            new Selector.Qualifier.Colors(new ColorMatcher.Not(Color.BLACK)),
                            new Selector.Qualifier.Types(TypeMatcher.CREATURE));
        }

        @Test
        void parsesHumanCreature() {
            var result = SelectorParsers.SELECTOR.parseSkipping(SPACE, "a Human creature");
            assertThat(result.objectType()).isEqualTo(GameObjectType.PERMANENT);
            assertThat(result.qualifiers())
                    .containsExactly(new Selector.Qualifier.Types(new TypeMatcher.All(
                            List.of(new TypeMatcher.IsSubtype(CreatureType.HUMAN), TypeMatcher.CREATURE))));
        }

        @Test
        void parsesNonHumanCreature() {
            var result = SelectorParsers.SELECTOR.parseSkipping(SPACE, "target non-Human creature");
            assertThat(result.qualifiers())
                    .containsExactly(
                            Selector.Qualifier.TARGET,
                            new Selector.Qualifier.Types(new TypeMatcher.All(List.of(
                                    new TypeMatcher.Not(new TypeMatcher.IsSubtype(CreatureType.HUMAN)),
                                    TypeMatcher.CREATURE))));
        }

        @Test
        void parsesNonDragonCreature() {
            var result = SelectorParsers.SELECTOR.parseSkipping(SPACE, "non-Dragon creature");
            assertThat(result.qualifiers())
                    .containsExactly(new Selector.Qualifier.Types(new TypeMatcher.All(List.of(
                            new TypeMatcher.Not(new TypeMatcher.IsSubtype(CreatureType.DRAGON)),
                            TypeMatcher.CREATURE))));
        }

        @Test
        void parsesMulticoloredPermanent() {
            var result = SelectorParsers.SELECTOR.parseSkipping(SPACE, "multicolored permanent");
            assertThat(result.qualifiers()).hasSize(1);
            assertThat(result.qualifiers().getFirst())
                    .isEqualTo(new Selector.Qualifier.Colors(ColorMatcher.MULTICOLORED));
        }

        @Test
        void parsesMonocoloredSpell() {
            var result = SelectorParsers.SELECTOR.parseSkipping(SPACE, "monocolored spell");
            assertThat(result.qualifiers()).hasSize(1);
            assertThat(result.qualifiers().getFirst())
                    .isEqualTo(new Selector.Qualifier.Colors(ColorMatcher.MONOCOLORED));
        }

        @Test
        void parsesColorlessPermanent() {
            var result = SelectorParsers.SELECTOR.parseSkipping(SPACE, "colorless permanent");
            assertThat(result.qualifiers()).hasSize(1);
            assertThat(result.qualifiers().getFirst()).isEqualTo(new Selector.Qualifier.Colors(ColorMatcher.COLORLESS));
        }

        @Test
        void parsesBlueOrGreenCreatureAsAny() {
            var result = SelectorParsers.SELECTOR.parseSkipping(SPACE, "target blue or green creature");
            assertThat(result.qualifiers())
                    .containsExactly(
                            Selector.Qualifier.TARGET,
                            new Selector.Qualifier.Colors(
                                    new ColorMatcher.Any(List.of(ColorMatcher.BLUE, ColorMatcher.GREEN))),
                            new Selector.Qualifier.Types(TypeMatcher.CREATURE));
        }

        @Test
        void parsesBlueAndOrGreenCreatureAsAny() {
            var result = SelectorParsers.SELECTOR.parseSkipping(SPACE, "blue and/or green creature");
            assertThat(result.qualifiers())
                    .containsExactly(
                            new Selector.Qualifier.Colors(
                                    new ColorMatcher.Any(List.of(ColorMatcher.BLUE, ColorMatcher.GREEN))),
                            new Selector.Qualifier.Types(TypeMatcher.CREATURE));
        }

        @Test
        void parsesNonblueNongreenCreatureFoldsIntoAll() {
            var result = SelectorParsers.SELECTOR.parseSkipping(SPACE, "target nonblue, nongreen creature");
            assertThat(result.qualifiers())
                    .containsExactly(
                            Selector.Qualifier.TARGET,
                            new Selector.Qualifier.Colors(
                                    new ColorMatcher.All(List.of(ColorMatcher.NON_BLUE, ColorMatcher.NON_GREEN))),
                            new Selector.Qualifier.Types(TypeMatcher.CREATURE));
        }

        @Test
        void parsesThreeNegatedColorsFoldsIntoAll() {
            var result = SelectorParsers.SELECTOR.parseSkipping(SPACE, "nonblue, nongreen, nonred creature");
            assertThat(result.qualifiers())
                    .containsExactly(
                            new Selector.Qualifier.Colors(new ColorMatcher.All(
                                    List.of(ColorMatcher.NON_BLUE, ColorMatcher.NON_GREEN, ColorMatcher.NON_RED))),
                            new Selector.Qualifier.Types(TypeMatcher.CREATURE));
        }

        @Test
        void mergeKeepsNonColorQualifiersInRelativeOrder() {
            var result = SelectorParsers.SELECTOR.parseSkipping(SPACE, "target tapped nonblue, nongreen creature");
            assertThat(result.qualifiers())
                    .containsExactly(
                            Selector.Qualifier.TARGET,
                            Selector.Qualifier.Status.TAPPED,
                            new Selector.Qualifier.Colors(
                                    new ColorMatcher.All(List.of(ColorMatcher.NON_BLUE, ColorMatcher.NON_GREEN))),
                            new Selector.Qualifier.Types(TypeMatcher.CREATURE));
        }

        @Test
        void mergeIsIdempotent() {
            var qs = List.<Selector.Qualifier>of(new Selector.Qualifier.Colors(
                    new ColorMatcher.All(List.of(ColorMatcher.NON_BLUE, ColorMatcher.NON_GREEN))));
            assertThat(ColorQualifierParsers.mergeColorQualifiers(qs)).isSameAs(qs);
        }

        @Test
        void mergeOnSingleColorIsNoOp() {
            var result = SelectorParsers.SELECTOR.parseSkipping(SPACE, "tapped nonblue creature");
            assertThat(result.qualifiers())
                    .containsExactly(
                            Selector.Qualifier.Status.TAPPED,
                            new Selector.Qualifier.Colors(ColorMatcher.NON_BLUE),
                            new Selector.Qualifier.Types(TypeMatcher.CREATURE));
        }
    }
}
