package be.imgn.mtg.engine.mana.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.mana.ManaSymbol;
import be.imgn.mtg.parse.Parser.ParseException;

/// Tests for ManaParser.MANA_COST parser to improve branch coverage.
class ManaParserManaCostTest {

    @Nested
    class SingleSymbols {

        @Test
        void parseWhite() {
            var cost = ManaParser.MANA_COST.parse("{W}");

            assertThat(cost.symbols()).hasSize(1);
            assertThat(cost.symbols().getFirst()).isInstanceOf(ManaSymbol.Colored.class);
            assertThat(cost.manaValue()).isEqualTo(1);
        }

        @Test
        void parseBlue() {
            var cost = ManaParser.MANA_COST.parse("{U}");

            assertThat(cost.symbols()).hasSize(1);
            assertThat(cost.manaValue()).isEqualTo(1);
        }

        @Test
        void parseBlack() {
            var cost = ManaParser.MANA_COST.parse("{B}");

            assertThat(cost.symbols()).hasSize(1);
            assertThat(cost.manaValue()).isEqualTo(1);
        }

        @Test
        void parseRed() {
            var cost = ManaParser.MANA_COST.parse("{R}");

            assertThat(cost.symbols()).hasSize(1);
            assertThat(cost.manaValue()).isEqualTo(1);
        }

        @Test
        void parseGreen() {
            var cost = ManaParser.MANA_COST.parse("{G}");

            assertThat(cost.symbols()).hasSize(1);
            assertThat(cost.manaValue()).isEqualTo(1);
        }

        @Test
        void parseColorless() {
            var cost = ManaParser.MANA_COST.parse("{C}");

            assertThat(cost.symbols()).hasSize(1);
            assertThat(cost.symbols().getFirst()).isEqualTo(ManaSymbol.Colorless.COLORLESS);
        }

        @Test
        void parseVariable() {
            var cost = ManaParser.MANA_COST.parse("{X}");

            assertThat(cost.symbols()).hasSize(1);
            assertThat(cost.symbols().getFirst()).isEqualTo(ManaSymbol.Variable.X);
            assertThat(cost.manaValue()).isZero();
        }

        @Test
        void parseSnow() {
            var cost = ManaParser.MANA_COST.parse("{S}");

            assertThat(cost.symbols()).hasSize(1);
            assertThat(cost.symbols().getFirst()).isEqualTo(ManaSymbol.Snow.SNOW);
        }

        @Test
        void parseGenericZero() {
            var cost = ManaParser.MANA_COST.parse("{0}");

            assertThat(cost.isEmpty()).isTrue();
        }

        @Test
        void parseGenericOne() {
            var cost = ManaParser.MANA_COST.parse("{1}");

            assertThat(cost.symbols()).hasSize(1);
            assertThat(cost.manaValue()).isEqualTo(1);
        }

        @Test
        void parseGenericLarge() {
            var cost = ManaParser.MANA_COST.parse("{16}");

            assertThat(cost.symbols()).hasSize(1);
            assertThat(cost.manaValue()).isEqualTo(16);
        }
    }

    @Nested
    class PhyrexianMana {

        @Test
        void parseWhitePhyrexian() {
            var cost = ManaParser.MANA_COST.parse("{W/P}");

            assertThat(cost.symbols()).hasSize(1);
            assertThat(cost.symbols().getFirst()).isEqualTo(ManaSymbol.Phyrexian.WHITE_PHYREXIAN);
        }

        @Test
        void parseBluePhyrexian() {
            var cost = ManaParser.MANA_COST.parse("{U/P}");

            assertThat(cost.symbols()).hasSize(1);
            assertThat(cost.symbols().getFirst()).isEqualTo(ManaSymbol.Phyrexian.BLUE_PHYREXIAN);
        }

        @Test
        void parseBlackPhyrexian() {
            var cost = ManaParser.MANA_COST.parse("{B/P}");

            assertThat(cost.symbols()).hasSize(1);
            assertThat(cost.symbols().getFirst()).isEqualTo(ManaSymbol.Phyrexian.BLACK_PHYREXIAN);
        }

        @Test
        void parseRedPhyrexian() {
            var cost = ManaParser.MANA_COST.parse("{R/P}");

            assertThat(cost.symbols()).hasSize(1);
            assertThat(cost.symbols().getFirst()).isEqualTo(ManaSymbol.Phyrexian.RED_PHYREXIAN);
        }

        @Test
        void parseGreenPhyrexian() {
            var cost = ManaParser.MANA_COST.parse("{G/P}");

            assertThat(cost.symbols()).hasSize(1);
            assertThat(cost.symbols().getFirst()).isEqualTo(ManaSymbol.Phyrexian.GREEN_PHYREXIAN);
        }
    }

    @Nested
    class HybridMana {

        @Test
        void parseWhiteBlue() {
            var cost = ManaParser.MANA_COST.parse("{W/U}");

            assertThat(cost.symbols()).hasSize(1);
            assertThat(cost.symbols().getFirst()).isEqualTo(ManaSymbol.Hybrid.WHITE_BLUE);
        }

        @Test
        void parseWhiteBlack() {
            var cost = ManaParser.MANA_COST.parse("{W/B}");

            assertThat(cost.symbols()).hasSize(1);
            assertThat(cost.symbols().getFirst()).isEqualTo(ManaSymbol.Hybrid.WHITE_BLACK);
        }

        @Test
        void parseBlueBlack() {
            var cost = ManaParser.MANA_COST.parse("{U/B}");

            assertThat(cost.symbols()).hasSize(1);
            assertThat(cost.symbols().getFirst()).isEqualTo(ManaSymbol.Hybrid.BLUE_BLACK);
        }

        @Test
        void parseBlueRed() {
            var cost = ManaParser.MANA_COST.parse("{U/R}");

            assertThat(cost.symbols()).hasSize(1);
            assertThat(cost.symbols().getFirst()).isEqualTo(ManaSymbol.Hybrid.BLUE_RED);
        }

        @Test
        void parseBlackRed() {
            var cost = ManaParser.MANA_COST.parse("{B/R}");

            assertThat(cost.symbols()).hasSize(1);
            assertThat(cost.symbols().getFirst()).isEqualTo(ManaSymbol.Hybrid.BLACK_RED);
        }

        @Test
        void parseBlackGreen() {
            var cost = ManaParser.MANA_COST.parse("{B/G}");

            assertThat(cost.symbols()).hasSize(1);
            assertThat(cost.symbols().getFirst()).isEqualTo(ManaSymbol.Hybrid.BLACK_GREEN);
        }

        @Test
        void parseRedGreen() {
            var cost = ManaParser.MANA_COST.parse("{R/G}");

            assertThat(cost.symbols()).hasSize(1);
            assertThat(cost.symbols().getFirst()).isEqualTo(ManaSymbol.Hybrid.RED_GREEN);
        }

        @Test
        void parseRedWhite() {
            var cost = ManaParser.MANA_COST.parse("{R/W}");

            assertThat(cost.symbols()).hasSize(1);
            assertThat(cost.symbols().getFirst()).isEqualTo(ManaSymbol.Hybrid.RED_WHITE);
        }

        @Test
        void parseGreenWhite() {
            var cost = ManaParser.MANA_COST.parse("{G/W}");

            assertThat(cost.symbols()).hasSize(1);
            assertThat(cost.symbols().getFirst()).isEqualTo(ManaSymbol.Hybrid.GREEN_WHITE);
        }

        @Test
        void parseGreenBlue() {
            var cost = ManaParser.MANA_COST.parse("{G/U}");

            assertThat(cost.symbols()).hasSize(1);
            assertThat(cost.symbols().getFirst()).isEqualTo(ManaSymbol.Hybrid.GREEN_BLUE);
        }
    }

    @Nested
    class MonoColorHybridMana {

        @Test
        void parseTwoWhite() {
            var cost = ManaParser.MANA_COST.parse("{2/W}");

            assertThat(cost.symbols()).hasSize(1);
            assertThat(cost.symbols().getFirst()).isEqualTo(ManaSymbol.MonoColorHybrid.TWO_WHITE);
        }

        @Test
        void parseTwoBlue() {
            var cost = ManaParser.MANA_COST.parse("{2/U}");

            assertThat(cost.symbols()).hasSize(1);
            assertThat(cost.symbols().getFirst()).isEqualTo(ManaSymbol.MonoColorHybrid.TWO_BLUE);
        }

        @Test
        void parseTwoBlack() {
            var cost = ManaParser.MANA_COST.parse("{2/B}");

            assertThat(cost.symbols()).hasSize(1);
            assertThat(cost.symbols().getFirst()).isEqualTo(ManaSymbol.MonoColorHybrid.TWO_BLACK);
        }

        @Test
        void parseTwoRed() {
            var cost = ManaParser.MANA_COST.parse("{2/R}");

            assertThat(cost.symbols()).hasSize(1);
            assertThat(cost.symbols().getFirst()).isEqualTo(ManaSymbol.MonoColorHybrid.TWO_RED);
        }

        @Test
        void parseTwoGreen() {
            var cost = ManaParser.MANA_COST.parse("{2/G}");

            assertThat(cost.symbols()).hasSize(1);
            assertThat(cost.symbols().getFirst()).isEqualTo(ManaSymbol.MonoColorHybrid.TWO_GREEN);
        }
    }

    @Nested
    class ColorlessHybridMana {

        @Test
        void parseColorlessWhite() {
            var cost = ManaParser.MANA_COST.parse("{C/W}");

            assertThat(cost.symbols()).hasSize(1);
            assertThat(cost.symbols().getFirst()).isEqualTo(ManaSymbol.ColorlessHybrid.COLORLESS_WHITE);
        }

        @Test
        void parseColorlessBlue() {
            var cost = ManaParser.MANA_COST.parse("{C/U}");

            assertThat(cost.symbols()).hasSize(1);
            assertThat(cost.symbols().getFirst()).isEqualTo(ManaSymbol.ColorlessHybrid.COLORLESS_BLUE);
        }

        @Test
        void parseColorlessBlack() {
            var cost = ManaParser.MANA_COST.parse("{C/B}");

            assertThat(cost.symbols()).hasSize(1);
            assertThat(cost.symbols().getFirst()).isEqualTo(ManaSymbol.ColorlessHybrid.COLORLESS_BLACK);
        }

        @Test
        void parseColorlessRed() {
            var cost = ManaParser.MANA_COST.parse("{C/R}");

            assertThat(cost.symbols()).hasSize(1);
            assertThat(cost.symbols().getFirst()).isEqualTo(ManaSymbol.ColorlessHybrid.COLORLESS_RED);
        }

        @Test
        void parseColorlessGreen() {
            var cost = ManaParser.MANA_COST.parse("{C/G}");

            assertThat(cost.symbols()).hasSize(1);
            assertThat(cost.symbols().getFirst()).isEqualTo(ManaSymbol.ColorlessHybrid.COLORLESS_GREEN);
        }
    }

    @Nested
    class HybridPhyrexianMana {

        @Test
        void parseWhiteBluePhyrexian() {
            var cost = ManaParser.MANA_COST.parse("{W/U/P}");

            assertThat(cost.symbols()).hasSize(1);
            assertThat(cost.symbols().getFirst()).isEqualTo(ManaSymbol.HybridPhyrexian.WHITE_BLUE_PHYREXIAN);
        }

        @Test
        void parseWhiteBlackPhyrexian() {
            var cost = ManaParser.MANA_COST.parse("{W/B/P}");

            assertThat(cost.symbols()).hasSize(1);
            assertThat(cost.symbols().getFirst()).isEqualTo(ManaSymbol.HybridPhyrexian.WHITE_BLACK_PHYREXIAN);
        }

        @Test
        void parseBlueBlackPhyrexian() {
            var cost = ManaParser.MANA_COST.parse("{U/B/P}");

            assertThat(cost.symbols()).hasSize(1);
            assertThat(cost.symbols().getFirst()).isEqualTo(ManaSymbol.HybridPhyrexian.BLUE_BLACK_PHYREXIAN);
        }

        @Test
        void parseBlueRedPhyrexian() {
            var cost = ManaParser.MANA_COST.parse("{U/R/P}");

            assertThat(cost.symbols()).hasSize(1);
            assertThat(cost.symbols().getFirst()).isEqualTo(ManaSymbol.HybridPhyrexian.BLUE_RED_PHYREXIAN);
        }

        @Test
        void parseBlackRedPhyrexian() {
            var cost = ManaParser.MANA_COST.parse("{B/R/P}");

            assertThat(cost.symbols()).hasSize(1);
            assertThat(cost.symbols().getFirst()).isEqualTo(ManaSymbol.HybridPhyrexian.BLACK_RED_PHYREXIAN);
        }

        @Test
        void parseBlackGreenPhyrexian() {
            var cost = ManaParser.MANA_COST.parse("{B/G/P}");

            assertThat(cost.symbols()).hasSize(1);
            assertThat(cost.symbols().getFirst()).isEqualTo(ManaSymbol.HybridPhyrexian.BLACK_GREEN_PHYREXIAN);
        }

        @Test
        void parseRedGreenPhyrexian() {
            var cost = ManaParser.MANA_COST.parse("{R/G/P}");

            assertThat(cost.symbols()).hasSize(1);
            assertThat(cost.symbols().getFirst()).isEqualTo(ManaSymbol.HybridPhyrexian.RED_GREEN_PHYREXIAN);
        }

        @Test
        void parseRedWhitePhyrexian() {
            var cost = ManaParser.MANA_COST.parse("{R/W/P}");

            assertThat(cost.symbols()).hasSize(1);
            assertThat(cost.symbols().getFirst()).isEqualTo(ManaSymbol.HybridPhyrexian.RED_WHITE_PHYREXIAN);
        }

        @Test
        void parseGreenWhitePhyrexian() {
            var cost = ManaParser.MANA_COST.parse("{G/W/P}");

            assertThat(cost.symbols()).hasSize(1);
            assertThat(cost.symbols().getFirst()).isEqualTo(ManaSymbol.HybridPhyrexian.GREEN_WHITE_PHYREXIAN);
        }

        @Test
        void parseGreenBluePhyrexian() {
            var cost = ManaParser.MANA_COST.parse("{G/U/P}");

            assertThat(cost.symbols()).hasSize(1);
            assertThat(cost.symbols().getFirst()).isEqualTo(ManaSymbol.HybridPhyrexian.GREEN_BLUE_PHYREXIAN);
        }
    }

    @Nested
    class MultiplSymbols {

        @Test
        void parseTwoColoredSame() {
            var cost = ManaParser.MANA_COST.parse("{G}{G}");

            assertThat(cost.symbols()).hasSize(2);
            assertThat(cost.manaValue()).isEqualTo(2);
        }

        @Test
        void parseTwoColoredDifferent() {
            var cost = ManaParser.MANA_COST.parse("{W}{U}");

            assertThat(cost.symbols()).hasSize(2);
            assertThat(cost.manaValue()).isEqualTo(2);
        }

        @Test
        void parseGenericAndColored() {
            var cost = ManaParser.MANA_COST.parse("{2}{R}");

            assertThat(cost.symbols()).hasSize(2);
            assertThat(cost.manaValue()).isEqualTo(3);
        }

        @Test
        void parseVariableAndColored() {
            var cost = ManaParser.MANA_COST.parse("{X}{G}{G}");

            assertThat(cost.symbols()).hasSize(3);
            assertThat(cost.manaValue()).isEqualTo(2); // X = 0
        }

        @Test
        void parseComplex() {
            var cost = ManaParser.MANA_COST.parse("{X}{3}{W}{W}{U}");

            assertThat(cost.symbols()).hasSize(5);
            // X=0, {3}=3, {W}=1, {W}=1, {U}=1 → Total = 6
            assertThat(cost.manaValue()).isEqualTo(6);
        }

        @Test
        void parseManyGeneric() {
            var cost = ManaParser.MANA_COST.parse("{5}{3}{2}");

            assertThat(cost.symbols()).hasSize(3);
            assertThat(cost.genericComponent()).isEqualTo(10);
        }

        @Test
        void parseWithHybrid() {
            var cost = ManaParser.MANA_COST.parse("{2}{W/U}{B}");

            assertThat(cost.symbols()).hasSize(3);
            assertThat(cost.manaValue()).isEqualTo(4);
        }

        @Test
        void parseWithPhyrexian() {
            var cost = ManaParser.MANA_COST.parse("{1}{G/P}{G/P}");

            assertThat(cost.symbols()).hasSize(3);
            assertThat(cost.manaValue()).isEqualTo(3);
        }

        @Test
        void parseAllTypes() {
            var cost = ManaParser.MANA_COST.parse("{X}{3}{C}{S}{G}{W/U}{2/R}{C/B}{W/U/P}");

            assertThat(cost.symbols()).hasSize(9);
        }
    }

    @Nested
    class ErrorCases {

        @Test
        void invalidSymbol() {
            assertThatThrownBy(() -> ManaParser.MANA_COST.parse("{Z}")).isInstanceOf(ParseException.class);
        }

        @Test
        void missingBrace() {
            assertThatThrownBy(() -> ManaParser.MANA_COST.parse("{G")).isInstanceOf(ParseException.class);
        }

        @Test
        void invalidHybridCombination() {
            // Invalid color pair (not adjacent on color wheel)
            assertThatThrownBy(() -> ManaParser.MANA_COST.parse("{W/R}")).isInstanceOf(ParseException.class);
        }

        @Test
        void emptyBraces() {
            assertThatThrownBy(() -> ManaParser.MANA_COST.parse("{}")).isInstanceOf(ParseException.class);
        }

        @Test
        void noBraces() {
            assertThatThrownBy(() -> ManaParser.MANA_COST.parse("G")).isInstanceOf(ParseException.class);
        }
    }

    @Nested
    class RealCardCosts {

        @Test
        void lightningBolt() {
            var cost = ManaParser.MANA_COST.parse("{R}");

            assertThat(cost.manaValue()).isEqualTo(1);
        }

        @Test
        void counterspell() {
            var cost = ManaParser.MANA_COST.parse("{U}{U}");

            assertThat(cost.manaValue()).isEqualTo(2);
        }

        @Test
        void wurmcoilEngine() {
            var cost = ManaParser.MANA_COST.parse("{6}");

            assertThat(cost.manaValue()).isEqualTo(6);
            assertThat(cost.genericComponent()).isEqualTo(6);
        }

        @Test
        void crypticCommand() {
            var cost = ManaParser.MANA_COST.parse("{1}{U}{U}{U}");

            assertThat(cost.manaValue()).isEqualTo(4);
        }

        @Test
        void hydroidKrasis() {
            var cost = ManaParser.MANA_COST.parse("{X}{G}{U}");

            assertThat(cost.manaValue()).isEqualTo(2);
            assertThat(cost.hasVariable()).isTrue();
        }

        @Test
        void gitaxianProbe() {
            var cost = ManaParser.MANA_COST.parse("{U/P}");

            assertThat(cost.symbols()).hasSize(1);
            assertThat(cost.symbols().getFirst()).isInstanceOf(ManaSymbol.Phyrexian.class);
        }

        @Test
        void fireIce() {
            var cost = ManaParser.MANA_COST.parse("{1}{U/R}");

            assertThat(cost.manaValue()).isEqualTo(2);
            assertThat(cost.symbols()).hasSize(2);
        }

        @Test
        void reaper_king() {
            var cost = ManaParser.MANA_COST.parse("{2/W}{2/U}{2/B}{2/R}{2/G}");

            // Each {2/X} mono-color hybrid has mana value 2 → Total = 10
            assertThat(cost.manaValue()).isEqualTo(10);
            assertThat(cost.symbols()).hasSize(5);
        }

        @Test
        void emrakul() {
            var cost = ManaParser.MANA_COST.parse("{15}");

            assertThat(cost.manaValue()).isEqualTo(15);
        }

        @Test
        void ornithopter() {
            var cost = ManaParser.MANA_COST.parse("{0}");

            assertThat(cost.isEmpty()).isTrue();
        }
    }
}
