package be.imgn.mtg.engine.oracle2.parser.ability;

import static be.imgn.mtg.engine.oracle2.domain.selector.PlayerRelationSelector.YOU;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import com.google.mu.util.CharPredicate;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.oracle2.domain.Amount;
import be.imgn.mtg.engine.oracle2.domain.AmountMatcher;
import be.imgn.mtg.engine.oracle2.domain.CardType;
import be.imgn.mtg.engine.oracle2.domain.Condition;
import be.imgn.mtg.engine.oracle2.domain.PlayerRelation;
import be.imgn.mtg.engine.oracle2.domain.ability.Ability;
import be.imgn.mtg.engine.oracle2.domain.ability.Cost;
import be.imgn.mtg.engine.oracle2.domain.ability.TriggerEvent;
import be.imgn.mtg.engine.oracle2.domain.effect.DestroyEffect;
import be.imgn.mtg.engine.oracle2.domain.effect.DrawEffect;
import be.imgn.mtg.engine.oracle2.domain.mana.ManaSymbol.Colored;
import be.imgn.mtg.engine.oracle2.domain.mana.ManaSymbol.Generic;
import be.imgn.mtg.engine.oracle2.domain.selector.CardTypeSelector;
import be.imgn.mtg.engine.oracle2.domain.selector.ObjectSelector;
import be.imgn.mtg.engine.oracle2.domain.selector.ObjectTypeSelector;
import be.imgn.mtg.engine.oracle2.domain.selector.PlayerRelationSelector;
import be.imgn.mtg.engine.oracle2.domain.selector.QuantifierSelector;
import be.imgn.mtg.engine.oracle2.domain.selector.Selector;
import be.imgn.mtg.engine.oracle2.domain.selector.SelfSelector;
import be.imgn.mtg.engine.oracle2.domain.selector.ZoneSelector;
import be.imgn.mtg.engine.turn.Step;

class AbilityParserTest {

    private static Ability parse(String input) {
        return AbilityParser.ABILITY.parseSkipping(CharPredicate.is(' '), input);
    }

    private static List<Ability> parseParagraph(String input) {
        return AbilityParser.PARAGRAPH.parseSkipping(CharPredicate.is(' '), input);
    }

    private static Selector quant1(Selector inner) {
        return new QuantifierSelector(new Amount.Exact(1), inner);
    }

    private static final Selector SELF = quant1(SelfSelector.SELF);
    private static final ZoneSelector.Battlefield CREATURE =
            new ZoneSelector.Battlefield(new ObjectTypeSelector.Permanent(new CardTypeSelector.Is(CardType.CREATURE)));
    private static final DrawEffect DRAW_A_CARD = new DrawEffect(quant1(YOU), new Amount.Exact(1));

    @Nested
    class Triggered {
        @Test
        void whenEntersDrawACard() {
            assertThat(parse("When ~ enters, draw a card."))
                    .isEqualTo(new Ability.TriggeredAbility.When(new TriggerEvent.Enters(SELF), List.of(DRAW_A_CARD)));
        }

        @Test
        void wheneverDiesDrawACard() {
            assertThat(parse("Whenever ~ dies, draw a card."))
                    .isEqualTo(
                            new Ability.TriggeredAbility.Whenever(new TriggerEvent.Dies(SELF), List.of(DRAW_A_CARD)));
        }

        @Test
        void atTheBeginningOfYourUpkeepDrawACard() {
            assertThat(parse("At the beginning of your upkeep, draw a card."))
                    .isEqualTo(new Ability.TriggeredAbility.At(
                            new TriggerEvent.AtBeginningOf(Step.UPKEEP), List.of(DRAW_A_CARD)));
        }

        /// Felidar Sovereign-style intervening-if: "At the beginning
        /// of your upkeep, if you have 40 or more life, [effect]."
        @Test
        void atUpkeepWithInterveningIfYouHave40OrMoreLife() {
            assertThat(parse("At the beginning of your upkeep, if you have 40 or more life, draw a card."))
                    .isEqualTo(new Ability.TriggeredAbility.At(
                            new TriggerEvent.AtBeginningOf(Step.UPKEEP),
                            new Condition.HasLife(
                                    quant1(new PlayerRelationSelector(PlayerRelation.YOU)),
                                    new AmountMatcher.AtLeast(new Amount.Exact(40))),
                            List.of(DRAW_A_CARD)));
        }

        /// Test of Endurance-style intervening-if with a different
        /// threshold — same shape as Felidar Sovereign with 50 life.
        @Test
        void atUpkeepWithInterveningIfYouHave50OrMoreLife() {
            assertThat(parse("At the beginning of your upkeep, if you have 50 or more life, draw a card."))
                    .isEqualTo(new Ability.TriggeredAbility.At(
                            new TriggerEvent.AtBeginningOf(Step.UPKEEP),
                            new Condition.HasLife(
                                    quant1(new PlayerRelationSelector(PlayerRelation.YOU)),
                                    new AmountMatcher.AtLeast(new Amount.Exact(50))),
                            List.of(DRAW_A_CARD)));
        }
    }

    @Nested
    class Activated {
        @Test
        void tapDrawACard() {
            assertThat(parse("{T}: Draw a card."))
                    .isEqualTo(new Ability.ActivatedAbility(new Cost.Tap(SelfSelector.SELF), List.of(DRAW_A_CARD)));
        }

        @Test
        void manaCostDrawACard() {
            assertThat(parse("{1}{U}: Draw a card."))
                    .isEqualTo(new Ability.ActivatedAbility(
                            new Cost.ManaCost(List.of(new Generic(1), Colored.BLUE)), List.of(DRAW_A_CARD)));
        }

        @Test
        void tapSacrificeDrawACard() {
            assertThat(parse("{T}, Sacrifice a creature: Draw a card."))
                    .isEqualTo(new Ability.ActivatedAbility(
                            new Cost.CompoundCost(
                                    List.of(new Cost.Tap(SelfSelector.SELF), new Cost.Sacrifice(quant1(CREATURE)))),
                            List.of(DRAW_A_CARD)));
        }
    }

    @Nested
    class Spell {
        @Test
        void destroyTargetCreature() {
            assertThat(parse("Destroy target creature."))
                    .isEqualTo(new Ability.SpellAbility(
                            List.of(new DestroyEffect(quant1(new ObjectSelector.Target(CREATURE))))));
        }

        @Test
        void drawACard() {
            assertThat(parse("Draw a card.")).isEqualTo(new Ability.SpellAbility(List.of(DRAW_A_CARD)));
        }
    }

    @Nested
    class Keyword {
        @Test
        void singleStaticKeyword() {
            assertThat(parse("Flying")).isEqualTo(Ability.StaticKeyword.FLYING);
        }
    }

    @Nested
    class Paragraph {
        @Test
        void multiKeyword() {
            assertThat(parseParagraph("Flying, vigilance"))
                    .containsExactly(Ability.StaticKeyword.FLYING, Ability.StaticKeyword.VIGILANCE);
        }

        @Test
        void singleTriggered() {
            assertThat(parseParagraph("When ~ enters, draw a card."))
                    .containsExactly(
                            new Ability.TriggeredAbility.When(new TriggerEvent.Enters(SELF), List.of(DRAW_A_CARD)));
        }

        @Test
        void singleSpell() {
            assertThat(parseParagraph("Destroy target creature."))
                    .containsExactly(new Ability.SpellAbility(
                            List.of(new DestroyEffect(quant1(new ObjectSelector.Target(CREATURE))))));
        }
    }
}
