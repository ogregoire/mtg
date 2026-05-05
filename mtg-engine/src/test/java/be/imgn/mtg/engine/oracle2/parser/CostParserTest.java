package be.imgn.mtg.engine.oracle2.parser;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import com.google.mu.util.CharPredicate;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.oracle2.domain.Amount;
import be.imgn.mtg.engine.oracle2.domain.CardType;
import be.imgn.mtg.engine.oracle2.domain.Cost;
import be.imgn.mtg.engine.oracle2.domain.selector.CardTypeSelector;
import be.imgn.mtg.engine.oracle2.domain.selector.ObjectTypeSelector;
import be.imgn.mtg.engine.oracle2.domain.selector.QuantifierSelector;
import be.imgn.mtg.engine.oracle2.domain.selector.Selector;
import be.imgn.mtg.engine.oracle2.domain.selector.ZoneSelector;

class CostParserTest {

    private static Cost parse(String input) {
        return CostParser.COST.parseSkipping(CharPredicate.is(' '), input);
    }

    private static final ZoneSelector.Battlefield CREATURE =
            new ZoneSelector.Battlefield(new ObjectTypeSelector.Permanent(new CardTypeSelector.Is(CardType.CREATURE)));

    private static Selector one(Selector inner) {
        return new QuantifierSelector(new Amount.Exact(1), inner);
    }

    @Nested
    class Primitive {
        @Test
        void tapSelf() {
            assertThat(parse("{T}")).isEqualTo(Cost.TapSelf.TAP_SELF);
        }

        @Test
        void manaCostGenericAndColor() {
            assertThat(parse("{1}{G}")).isEqualTo(new Cost.ManaCost("{1}{G}"));
        }

        @Test
        void manaCostWithVariable() {
            assertThat(parse("{X}{R}")).isEqualTo(new Cost.ManaCost("{X}{R}"));
        }

        @Test
        void sacrificeACreature() {
            assertThat(parse("Sacrifice a creature")).isEqualTo(new Cost.Sacrifice(one(CREATURE)));
        }

        @Test
        void payTwoLife() {
            assertThat(parse("Pay 2 life")).isEqualTo(new Cost.PayLife(new Amount.Exact(2)));
        }

        @Test
        void payXLife() {
            assertThat(parse("Pay X life")).isEqualTo(new Cost.PayLife(Amount.Standard.X));
        }
    }

    @Nested
    class Compound {
        @Test
        void manaThenTap() {
            assertThat(parse("{1}, {T}"))
                    .isEqualTo(new Cost.CompoundCost(List.of(new Cost.ManaCost("{1}"), Cost.TapSelf.TAP_SELF)));
        }

        @Test
        void manaTapAndSacrifice() {
            assertThat(parse("{1}{G}, {T}, Sacrifice a creature"))
                    .isEqualTo(new Cost.CompoundCost(List.of(
                            new Cost.ManaCost("{1}{G}"), Cost.TapSelf.TAP_SELF, new Cost.Sacrifice(one(CREATURE)))));
        }

        @Test
        void tapAndPayLife() {
            assertThat(parse("{T}, Pay 2 life"))
                    .isEqualTo(new Cost.CompoundCost(
                            List.of(Cost.TapSelf.TAP_SELF, new Cost.PayLife(new Amount.Exact(2)))));
        }

        @Test
        void singletonStaysBare() {
            // Make sure a singleton list doesn't accidentally wrap in CompoundCost.
            assertThat(parse("{2}{R}")).isInstanceOf(Cost.ManaCost.class);
        }
    }
}
