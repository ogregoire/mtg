package be.imgn.mtg.engine.oracle2.parser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import com.google.mu.util.CharPredicate;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.oracle2.domain.Amount;
import be.imgn.mtg.engine.oracle2.domain.CardType;
import be.imgn.mtg.engine.oracle2.domain.Cost;
import be.imgn.mtg.engine.oracle2.domain.mana.ManaSymbol;
import be.imgn.mtg.engine.oracle2.domain.mana.ManaSymbol.Colored;
import be.imgn.mtg.engine.oracle2.domain.mana.ManaSymbol.Colorless;
import be.imgn.mtg.engine.oracle2.domain.mana.ManaSymbol.ColorlessHybrid;
import be.imgn.mtg.engine.oracle2.domain.mana.ManaSymbol.Generic;
import be.imgn.mtg.engine.oracle2.domain.mana.ManaSymbol.Hybrid;
import be.imgn.mtg.engine.oracle2.domain.mana.ManaSymbol.HybridPhyrexian;
import be.imgn.mtg.engine.oracle2.domain.mana.ManaSymbol.MonoColorHybrid;
import be.imgn.mtg.engine.oracle2.domain.mana.ManaSymbol.Phyrexian;
import be.imgn.mtg.engine.oracle2.domain.mana.ManaSymbol.Snow;
import be.imgn.mtg.engine.oracle2.domain.mana.ManaSymbol.Variable;
import be.imgn.mtg.engine.oracle2.domain.selector.CardTypeSelector;
import be.imgn.mtg.engine.oracle2.domain.selector.ObjectTypeSelector;
import be.imgn.mtg.engine.oracle2.domain.selector.QuantifierSelector;
import be.imgn.mtg.engine.oracle2.domain.selector.Selector;
import be.imgn.mtg.engine.oracle2.domain.selector.ZoneSelector;

class CostParserTest {

    private static Cost parse(String input) {
        return CostParser.COST.parseSkipping(CharPredicate.is(' '), input);
    }

    private static ManaSymbol parseSymbol(String input) {
        return CostParser.MANA_SYMBOL.parse(input);
    }

    private static Cost.ManaCost mana(ManaSymbol... symbols) {
        return new Cost.ManaCost(List.of(symbols));
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
            assertThat(parse("{1}{G}")).isEqualTo(mana(new Generic(1), Colored.GREEN));
        }

        @Test
        void manaCostWithVariable() {
            assertThat(parse("{X}{R}")).isEqualTo(mana(Variable.X, Colored.RED));
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
                    .isEqualTo(new Cost.CompoundCost(List.of(mana(new Generic(1)), Cost.TapSelf.TAP_SELF)));
        }

        @Test
        void manaTapAndSacrifice() {
            assertThat(parse("{1}{G}, {T}, Sacrifice a creature"))
                    .isEqualTo(new Cost.CompoundCost(List.of(
                            mana(new Generic(1), Colored.GREEN),
                            Cost.TapSelf.TAP_SELF,
                            new Cost.Sacrifice(one(CREATURE)))));
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

    @Nested
    class ManaSymbols {

        @Test
        void colored() {
            assertThat(parseSymbol("{W}")).isEqualTo(Colored.WHITE);
            assertThat(parseSymbol("{U}")).isEqualTo(Colored.BLUE);
            assertThat(parseSymbol("{B}")).isEqualTo(Colored.BLACK);
            assertThat(parseSymbol("{R}")).isEqualTo(Colored.RED);
            assertThat(parseSymbol("{G}")).isEqualTo(Colored.GREEN);
        }

        @Test
        void colorless() {
            assertThat(parseSymbol("{C}")).isEqualTo(Colorless.COLORLESS);
        }

        @Test
        void variable() {
            assertThat(parseSymbol("{X}")).isEqualTo(Variable.X);
        }

        @Test
        void snow() {
            assertThat(parseSymbol("{S}")).isEqualTo(Snow.SNOW);
        }

        @Test
        void genericIncludingZero() {
            assertThat(parseSymbol("{0}")).isEqualTo(new Generic(0));
            assertThat(parseSymbol("{1}")).isEqualTo(new Generic(1));
            assertThat(parseSymbol("{12}")).isEqualTo(new Generic(12));
        }

        @Test
        void hybridAllTen() {
            assertThat(parseSymbol("{W/U}")).isEqualTo(Hybrid.WHITE_BLUE);
            assertThat(parseSymbol("{W/B}")).isEqualTo(Hybrid.WHITE_BLACK);
            assertThat(parseSymbol("{U/B}")).isEqualTo(Hybrid.BLUE_BLACK);
            assertThat(parseSymbol("{U/R}")).isEqualTo(Hybrid.BLUE_RED);
            assertThat(parseSymbol("{B/R}")).isEqualTo(Hybrid.BLACK_RED);
            assertThat(parseSymbol("{B/G}")).isEqualTo(Hybrid.BLACK_GREEN);
            assertThat(parseSymbol("{R/G}")).isEqualTo(Hybrid.RED_GREEN);
            assertThat(parseSymbol("{R/W}")).isEqualTo(Hybrid.RED_WHITE);
            assertThat(parseSymbol("{G/W}")).isEqualTo(Hybrid.GREEN_WHITE);
            assertThat(parseSymbol("{G/U}")).isEqualTo(Hybrid.GREEN_BLUE);
        }

        @Test
        void monoColorHybrid() {
            assertThat(parseSymbol("{2/W}")).isEqualTo(MonoColorHybrid.TWO_WHITE);
            assertThat(parseSymbol("{2/U}")).isEqualTo(MonoColorHybrid.TWO_BLUE);
            assertThat(parseSymbol("{2/B}")).isEqualTo(MonoColorHybrid.TWO_BLACK);
            assertThat(parseSymbol("{2/R}")).isEqualTo(MonoColorHybrid.TWO_RED);
            assertThat(parseSymbol("{2/G}")).isEqualTo(MonoColorHybrid.TWO_GREEN);
        }

        @Test
        void colorlessHybrid() {
            assertThat(parseSymbol("{C/W}")).isEqualTo(ColorlessHybrid.COLORLESS_WHITE);
            assertThat(parseSymbol("{C/U}")).isEqualTo(ColorlessHybrid.COLORLESS_BLUE);
            assertThat(parseSymbol("{C/B}")).isEqualTo(ColorlessHybrid.COLORLESS_BLACK);
            assertThat(parseSymbol("{C/R}")).isEqualTo(ColorlessHybrid.COLORLESS_RED);
            assertThat(parseSymbol("{C/G}")).isEqualTo(ColorlessHybrid.COLORLESS_GREEN);
        }

        @Test
        void phyrexian() {
            assertThat(parseSymbol("{W/P}")).isEqualTo(Phyrexian.WHITE);
            assertThat(parseSymbol("{U/P}")).isEqualTo(Phyrexian.BLUE);
            assertThat(parseSymbol("{B/P}")).isEqualTo(Phyrexian.BLACK);
            assertThat(parseSymbol("{R/P}")).isEqualTo(Phyrexian.RED);
            assertThat(parseSymbol("{G/P}")).isEqualTo(Phyrexian.GREEN);
        }

        @Test
        void hybridPhyrexianAllTen() {
            assertThat(parseSymbol("{W/U/P}")).isEqualTo(HybridPhyrexian.WHITE_BLUE);
            assertThat(parseSymbol("{W/B/P}")).isEqualTo(HybridPhyrexian.WHITE_BLACK);
            assertThat(parseSymbol("{U/B/P}")).isEqualTo(HybridPhyrexian.BLUE_BLACK);
            assertThat(parseSymbol("{U/R/P}")).isEqualTo(HybridPhyrexian.BLUE_RED);
            assertThat(parseSymbol("{B/R/P}")).isEqualTo(HybridPhyrexian.BLACK_RED);
            assertThat(parseSymbol("{B/G/P}")).isEqualTo(HybridPhyrexian.BLACK_GREEN);
            assertThat(parseSymbol("{R/G/P}")).isEqualTo(HybridPhyrexian.RED_GREEN);
            assertThat(parseSymbol("{R/W/P}")).isEqualTo(HybridPhyrexian.RED_WHITE);
            assertThat(parseSymbol("{G/W/P}")).isEqualTo(HybridPhyrexian.GREEN_WHITE);
            assertThat(parseSymbol("{G/U/P}")).isEqualTo(HybridPhyrexian.GREEN_BLUE);
        }

        @Test
        void hybridVsHybridPhyrexianDisambiguation() {
            assertThat(parseSymbol("{W/U}")).isEqualTo(Hybrid.WHITE_BLUE);
            assertThat(parseSymbol("{W/U/P}")).isEqualTo(HybridPhyrexian.WHITE_BLUE);
            assertThat(parseSymbol("{W/P}")).isEqualTo(Phyrexian.WHITE);
        }

        @Test
        void monaCostMixedSymbols() {
            assertThat(parse("{2}{W}{W}")).isEqualTo(mana(new Generic(2), Colored.WHITE, Colored.WHITE));
            assertThat(parse("{X}{B/R}{B/R}")).isEqualTo(mana(Variable.X, Hybrid.BLACK_RED, Hybrid.BLACK_RED));
            assertThat(parse("{2/W}{W/P}")).isEqualTo(mana(MonoColorHybrid.TWO_WHITE, Phyrexian.WHITE));
        }

        @Test
        void rejectsGarbage() {
            assertThatThrownBy(() -> parseSymbol("{XYZ}")).isInstanceOf(RuntimeException.class);
            assertThatThrownBy(() -> parseSymbol("{}")).isInstanceOf(RuntimeException.class);
            assertThatThrownBy(() -> parseSymbol("{W/}")).isInstanceOf(RuntimeException.class);
            assertThatThrownBy(() -> parseSymbol("{/W}")).isInstanceOf(RuntimeException.class);
            assertThatThrownBy(() -> parseSymbol("{W/Z}")).isInstanceOf(RuntimeException.class);
        }
    }
}
