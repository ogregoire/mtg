package be.imgn.mtg.engine.oracle2.parser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

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

class ManaParserTest {

    private static ManaSymbol parseSymbol(String input) {
        return ManaParser.SYMBOL.parse(input);
    }

    private static List<ManaSymbol> parseSymbols(String input) {
        return ManaParser.SYMBOLS.parse(input);
    }

    @Nested
    class Symbol {
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
        void rejectsGarbage() {
            assertThatThrownBy(() -> parseSymbol("{XYZ}")).isInstanceOf(RuntimeException.class);
            assertThatThrownBy(() -> parseSymbol("{}")).isInstanceOf(RuntimeException.class);
            assertThatThrownBy(() -> parseSymbol("{W/}")).isInstanceOf(RuntimeException.class);
            assertThatThrownBy(() -> parseSymbol("{/W}")).isInstanceOf(RuntimeException.class);
            assertThatThrownBy(() -> parseSymbol("{W/Z}")).isInstanceOf(RuntimeException.class);
        }
    }

    @Nested
    class Symbols {
        @Test
        void singleSymbol() {
            assertThat(parseSymbols("{W}")).containsExactly(Colored.WHITE);
        }

        @Test
        void mixedTypicalManaCost() {
            assertThat(parseSymbols("{2}{W}{W}")).containsExactly(new Generic(2), Colored.WHITE, Colored.WHITE);
        }

        @Test
        void hybridsSpread() {
            assertThat(parseSymbols("{X}{B/R}{B/R}")).containsExactly(Variable.X, Hybrid.BLACK_RED, Hybrid.BLACK_RED);
        }

        @Test
        void monoColorAndPhyrexianMixed() {
            assertThat(parseSymbols("{2/W}{W/P}")).containsExactly(MonoColorHybrid.TWO_WHITE, Phyrexian.WHITE);
        }

        @Test
        void preservesOracleOrder() {
            assertThat(parseSymbols("{G}{1}{W}")).containsExactly(Colored.GREEN, new Generic(1), Colored.WHITE);
        }

        @Test
        void rejectsEmpty() {
            assertThatThrownBy(() -> parseSymbols("")).isInstanceOf(RuntimeException.class);
        }
    }
}
