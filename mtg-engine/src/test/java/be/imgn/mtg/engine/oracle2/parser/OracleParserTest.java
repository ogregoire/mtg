package be.imgn.mtg.engine.oracle2.parser;

import static be.imgn.mtg.engine.oracle2.domain.selector.PlayerRelationSelector.YOU;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.oracle2.domain.Amount;
import be.imgn.mtg.engine.oracle2.domain.CardType;
import be.imgn.mtg.engine.oracle2.domain.ability.Ability;
import be.imgn.mtg.engine.oracle2.domain.ability.Cost;
import be.imgn.mtg.engine.oracle2.domain.ability.TriggerEvent;
import be.imgn.mtg.engine.oracle2.domain.effect.DestroyEffect;
import be.imgn.mtg.engine.oracle2.domain.effect.DrawEffect;
import be.imgn.mtg.engine.oracle2.domain.selector.CardTypeSelector;
import be.imgn.mtg.engine.oracle2.domain.selector.ObjectSelector;
import be.imgn.mtg.engine.oracle2.domain.selector.ObjectTypeSelector;
import be.imgn.mtg.engine.oracle2.domain.selector.QuantifierSelector;
import be.imgn.mtg.engine.oracle2.domain.selector.Selector;
import be.imgn.mtg.engine.oracle2.domain.selector.SelfSelector;
import be.imgn.mtg.engine.oracle2.domain.selector.ZoneSelector;

class OracleParserTest {

    private static Selector quant1(Selector inner) {
        return new QuantifierSelector(new Amount.Exact(1), inner);
    }

    private static final Selector SELF = quant1(SelfSelector.SELF);
    private static final ZoneSelector.Battlefield CREATURE =
            new ZoneSelector.Battlefield(new ObjectTypeSelector.Permanent(new CardTypeSelector.Is(CardType.CREATURE)));
    private static final DrawEffect DRAW_A_CARD = new DrawEffect(quant1(YOU), new Amount.Exact(1));

    @Nested
    class Spell {
        @Test
        void destroyTargetCreature() {
            assertThat(OracleParser.parse("Murder", "Destroy target creature."))
                    .containsExactly(new Ability.SpellAbility(
                            List.of(new DestroyEffect(quant1(new ObjectSelector.Target(CREATURE))))));
        }
    }

    @Nested
    class Activated {
        @Test
        void tapDrawACard() {
            // Library of Alexandria-style — name doesn't appear in text so no `~` substitution.
            assertThat(OracleParser.parse("Inspiring Statuary", "{T}: Draw a card."))
                    .containsExactly(
                            new Ability.ActivatedAbility(new Cost.Tap(SelfSelector.SELF), List.of(DRAW_A_CARD)));
        }
    }

    @Nested
    class Triggered {
        @Test
        void whenSelfEntersDrawACard() {
            // Mulldrifter-style — card name self-substitutes to ~.
            assertThat(OracleParser.parse("Mulldrifter", "When Mulldrifter enters, draw a card."))
                    .containsExactly(
                            new Ability.TriggeredAbility.When(new TriggerEvent.Enters(SELF), List.of(DRAW_A_CARD)));
        }
    }

    @Nested
    class KeywordLine {
        @Test
        void serraAngelStyle() {
            // Just the keyword paragraph; richer body deferred.
            assertThat(OracleParser.parse("Serra Angel", "Flying, vigilance"))
                    .containsExactly(Ability.StaticKeyword.FLYING, Ability.StaticKeyword.VIGILANCE);
        }
    }

    @Nested
    class MultiParagraph {
        @Test
        void keywordPlusActivated() {
            assertThat(OracleParser.parse("Generic Card", "Flying\n{T}: Draw a card."))
                    .containsExactly(
                            Ability.StaticKeyword.FLYING,
                            new Ability.ActivatedAbility(new Cost.Tap(SelfSelector.SELF), List.of(DRAW_A_CARD)));
        }

        @Test
        void triggeredPlusActivated() {
            assertThat(OracleParser.parse("Mulldrifter", "When Mulldrifter enters, draw a card.\n{T}: Draw a card."))
                    .containsExactly(
                            new Ability.TriggeredAbility.When(new TriggerEvent.Enters(SELF), List.of(DRAW_A_CARD)),
                            new Ability.ActivatedAbility(new Cost.Tap(SelfSelector.SELF), List.of(DRAW_A_CARD)));
        }
    }

    @Nested
    class EmptyAndBlank {
        @Test
        void empty() {
            assertThat(OracleParser.parse("X", "")).isEmpty();
        }

        @Test
        void blank() {
            assertThat(OracleParser.parse("X", "   ")).isEmpty();
        }
    }
}
