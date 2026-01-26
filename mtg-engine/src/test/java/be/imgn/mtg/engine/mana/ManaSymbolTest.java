package be.imgn.mtg.engine.mana;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.characteristics.Color;
import be.imgn.mtg.engine.characteristics.Colors;

class ManaSymbolTest {

    @Nested
    class ColoredSymbol {

        @Test
        void whiteSymbol() {
            var symbol = ManaSymbol.Colored.WHITE;
            assertThat(symbol.manaValue()).isEqualTo(1);
            assertThat(symbol.notation()).isEqualTo("{W}");
            assertThat(symbol.manaType()).isEqualTo(ManaType.WHITE);
            assertThat(symbol.color()).isEqualTo(Color.WHITE);
            assertThat(symbol.colors()).isEqualTo(Colors.of(Color.WHITE));
        }

        @Test
        void blueSymbol() {
            var symbol = ManaSymbol.Colored.BLUE;
            assertThat(symbol.manaValue()).isEqualTo(1);
            assertThat(symbol.notation()).isEqualTo("{U}");
            assertThat(symbol.manaType()).isEqualTo(ManaType.BLUE);
            assertThat(symbol.color()).isEqualTo(Color.BLUE);
        }

        @Test
        void blackSymbol() {
            var symbol = ManaSymbol.Colored.BLACK;
            assertThat(symbol.manaValue()).isEqualTo(1);
            assertThat(symbol.notation()).isEqualTo("{B}");
            assertThat(symbol.manaType()).isEqualTo(ManaType.BLACK);
            assertThat(symbol.color()).isEqualTo(Color.BLACK);
        }

        @Test
        void redSymbol() {
            var symbol = ManaSymbol.Colored.RED;
            assertThat(symbol.manaValue()).isEqualTo(1);
            assertThat(symbol.notation()).isEqualTo("{R}");
            assertThat(symbol.manaType()).isEqualTo(ManaType.RED);
            assertThat(symbol.color()).isEqualTo(Color.RED);
        }

        @Test
        void greenSymbol() {
            var symbol = ManaSymbol.Colored.GREEN;
            assertThat(symbol.manaValue()).isEqualTo(1);
            assertThat(symbol.notation()).isEqualTo("{G}");
            assertThat(symbol.manaType()).isEqualTo(ManaType.GREEN);
            assertThat(symbol.color()).isEqualTo(Color.GREEN);
        }

        @Test
        void fromManaTypeWhite() {
            var symbol = ManaSymbol.Colored.fromManaType(ManaType.WHITE);
            assertThat(symbol).isEqualTo(ManaSymbol.Colored.WHITE);
        }

        @Test
        void fromManaTypeBlue() {
            var symbol = ManaSymbol.Colored.fromManaType(ManaType.BLUE);
            assertThat(symbol).isEqualTo(ManaSymbol.Colored.BLUE);
        }

        @Test
        void fromManaTypeBlack() {
            var symbol = ManaSymbol.Colored.fromManaType(ManaType.BLACK);
            assertThat(symbol).isEqualTo(ManaSymbol.Colored.BLACK);
        }

        @Test
        void fromManaTypeRed() {
            var symbol = ManaSymbol.Colored.fromManaType(ManaType.RED);
            assertThat(symbol).isEqualTo(ManaSymbol.Colored.RED);
        }

        @Test
        void fromManaTypeGreen() {
            var symbol = ManaSymbol.Colored.fromManaType(ManaType.GREEN);
            assertThat(symbol).isEqualTo(ManaSymbol.Colored.GREEN);
        }
    }

    @Nested
    class ColorlessSymbol {

        @Test
        void colorlessSymbol() {
            var symbol = ManaSymbol.Colorless.COLORLESS;
            assertThat(symbol.manaValue()).isEqualTo(1);
            assertThat(symbol.notation()).isEqualTo("{C}");
            assertThat(symbol.colors()).isEqualTo(Colors.empty());
        }
    }

    @Nested
    class GenericSymbol {

        @Test
        void zeroGeneric() {
            var symbol = new ManaSymbol.Generic(0);
            assertThat(symbol.manaValue()).isZero();
            assertThat(symbol.notation()).isEqualTo("{0}");
            assertThat(symbol.colors()).isEqualTo(Colors.empty());
        }

        @Test
        void oneGeneric() {
            var symbol = new ManaSymbol.Generic(1);
            assertThat(symbol.manaValue()).isEqualTo(1);
            assertThat(symbol.notation()).isEqualTo("{1}");
        }

        @Test
        void largeGeneric() {
            var symbol = new ManaSymbol.Generic(15);
            assertThat(symbol.manaValue()).isEqualTo(15);
            assertThat(symbol.notation()).isEqualTo("{15}");
        }

        @Test
        void negativeAmountThrows() {
            assertThatThrownBy(() -> new ManaSymbol.Generic(-1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("cannot be negative");
        }
    }

    @Nested
    class VariableSymbol {

        @Test
        void xSymbol() {
            var symbol = ManaSymbol.Variable.X;
            assertThat(symbol.manaValue()).isZero();
            assertThat(symbol.notation()).isEqualTo("{X}");
            assertThat(symbol.colors()).isEqualTo(Colors.empty());
        }
    }

    @Nested
    class SnowSymbol {

        @Test
        void snowSymbol() {
            var symbol = ManaSymbol.Snow.SNOW;
            assertThat(symbol.manaValue()).isEqualTo(1);
            assertThat(symbol.notation()).isEqualTo("{S}");
            assertThat(symbol.colors()).isEqualTo(Colors.empty());
        }
    }

    @Nested
    class PhyrexianSymbol {

        @Test
        void whitePhyrexian() {
            var symbol = ManaSymbol.Phyrexian.WHITE_PHYREXIAN;
            assertThat(symbol.manaValue()).isEqualTo(1);
            assertThat(symbol.notation()).isEqualTo("{W/P}");
            assertThat(symbol.manaType()).isEqualTo(ManaType.WHITE);
            assertThat(symbol.colors()).isEqualTo(Colors.of(Color.WHITE));
        }

        @Test
        void bluePhyrexian() {
            var symbol = ManaSymbol.Phyrexian.BLUE_PHYREXIAN;
            assertThat(symbol.notation()).isEqualTo("{U/P}");
            assertThat(symbol.manaType()).isEqualTo(ManaType.BLUE);
        }

        @Test
        void blackPhyrexian() {
            var symbol = ManaSymbol.Phyrexian.BLACK_PHYREXIAN;
            assertThat(symbol.notation()).isEqualTo("{B/P}");
            assertThat(symbol.manaType()).isEqualTo(ManaType.BLACK);
        }

        @Test
        void redPhyrexian() {
            var symbol = ManaSymbol.Phyrexian.RED_PHYREXIAN;
            assertThat(symbol.notation()).isEqualTo("{R/P}");
            assertThat(symbol.manaType()).isEqualTo(ManaType.RED);
        }

        @Test
        void greenPhyrexian() {
            var symbol = ManaSymbol.Phyrexian.GREEN_PHYREXIAN;
            assertThat(symbol.notation()).isEqualTo("{G/P}");
            assertThat(symbol.manaType()).isEqualTo(ManaType.GREEN);
        }
    }

    @Nested
    class HybridPhyrexianSymbol {

        @Test
        void whiteBluePhyrexian() {
            var symbol = ManaSymbol.HybridPhyrexian.WHITE_BLUE_PHYREXIAN;
            assertThat(symbol.manaValue()).isEqualTo(1);
            assertThat(symbol.notation()).isEqualTo("{W/U/P}");
            assertThat(symbol.option1()).isEqualTo(ManaType.WHITE);
            assertThat(symbol.option2()).isEqualTo(ManaType.BLUE);
            assertThat(symbol.colors()).isEqualTo(Colors.of(Color.WHITE, Color.BLUE));
        }

        @Test
        void whiteBlackPhyrexian() {
            var symbol = ManaSymbol.HybridPhyrexian.WHITE_BLACK_PHYREXIAN;
            assertThat(symbol.notation()).isEqualTo("{W/B/P}");
        }

        @Test
        void blueBlackPhyrexian() {
            var symbol = ManaSymbol.HybridPhyrexian.BLUE_BLACK_PHYREXIAN;
            assertThat(symbol.notation()).isEqualTo("{U/B/P}");
        }

        @Test
        void blueRedPhyrexian() {
            var symbol = ManaSymbol.HybridPhyrexian.BLUE_RED_PHYREXIAN;
            assertThat(symbol.notation()).isEqualTo("{U/R/P}");
        }

        @Test
        void blackRedPhyrexian() {
            var symbol = ManaSymbol.HybridPhyrexian.BLACK_RED_PHYREXIAN;
            assertThat(symbol.notation()).isEqualTo("{B/R/P}");
        }

        @Test
        void blackGreenPhyrexian() {
            var symbol = ManaSymbol.HybridPhyrexian.BLACK_GREEN_PHYREXIAN;
            assertThat(symbol.notation()).isEqualTo("{B/G/P}");
        }

        @Test
        void redGreenPhyrexian() {
            var symbol = ManaSymbol.HybridPhyrexian.RED_GREEN_PHYREXIAN;
            assertThat(symbol.notation()).isEqualTo("{R/G/P}");
        }

        @Test
        void redWhitePhyrexian() {
            var symbol = ManaSymbol.HybridPhyrexian.RED_WHITE_PHYREXIAN;
            assertThat(symbol.notation()).isEqualTo("{R/W/P}");
        }

        @Test
        void greenWhitePhyrexian() {
            var symbol = ManaSymbol.HybridPhyrexian.GREEN_WHITE_PHYREXIAN;
            assertThat(symbol.notation()).isEqualTo("{G/W/P}");
        }

        @Test
        void greenBluePhyrexian() {
            var symbol = ManaSymbol.HybridPhyrexian.GREEN_BLUE_PHYREXIAN;
            assertThat(symbol.notation()).isEqualTo("{G/U/P}");
        }
    }

    @Nested
    class HybridSymbol {

        @Test
        void whiteBlue() {
            var symbol = ManaSymbol.Hybrid.WHITE_BLUE;
            assertThat(symbol.manaValue()).isEqualTo(1);
            assertThat(symbol.notation()).isEqualTo("{W/U}");
            assertThat(symbol.option1()).isEqualTo(ManaType.WHITE);
            assertThat(symbol.option2()).isEqualTo(ManaType.BLUE);
            assertThat(symbol.colors()).isEqualTo(Colors.of(Color.WHITE, Color.BLUE));
        }

        @Test
        void whiteBlack() {
            var symbol = ManaSymbol.Hybrid.WHITE_BLACK;
            assertThat(symbol.notation()).isEqualTo("{W/B}");
        }

        @Test
        void blueBlack() {
            var symbol = ManaSymbol.Hybrid.BLUE_BLACK;
            assertThat(symbol.notation()).isEqualTo("{U/B}");
        }

        @Test
        void blueRed() {
            var symbol = ManaSymbol.Hybrid.BLUE_RED;
            assertThat(symbol.notation()).isEqualTo("{U/R}");
        }

        @Test
        void blackRed() {
            var symbol = ManaSymbol.Hybrid.BLACK_RED;
            assertThat(symbol.notation()).isEqualTo("{B/R}");
        }

        @Test
        void blackGreen() {
            var symbol = ManaSymbol.Hybrid.BLACK_GREEN;
            assertThat(symbol.notation()).isEqualTo("{B/G}");
        }

        @Test
        void redGreen() {
            var symbol = ManaSymbol.Hybrid.RED_GREEN;
            assertThat(symbol.notation()).isEqualTo("{R/G}");
        }

        @Test
        void redWhite() {
            var symbol = ManaSymbol.Hybrid.RED_WHITE;
            assertThat(symbol.notation()).isEqualTo("{R/W}");
        }

        @Test
        void greenWhite() {
            var symbol = ManaSymbol.Hybrid.GREEN_WHITE;
            assertThat(symbol.notation()).isEqualTo("{G/W}");
        }

        @Test
        void greenBlue() {
            var symbol = ManaSymbol.Hybrid.GREEN_BLUE;
            assertThat(symbol.notation()).isEqualTo("{G/U}");
        }
    }

    @Nested
    class MonoColorHybridSymbol {

        @Test
        void twoWhite() {
            var symbol = ManaSymbol.MonoColorHybrid.TWO_WHITE;
            assertThat(symbol.manaValue()).isEqualTo(2);
            assertThat(symbol.notation()).isEqualTo("{2/W}");
            assertThat(symbol.colorOption()).isEqualTo(ManaType.WHITE);
            assertThat(symbol.colors()).isEqualTo(Colors.of(Color.WHITE));
        }

        @Test
        void twoBlue() {
            var symbol = ManaSymbol.MonoColorHybrid.TWO_BLUE;
            assertThat(symbol.notation()).isEqualTo("{2/U}");
            assertThat(symbol.colorOption()).isEqualTo(ManaType.BLUE);
        }

        @Test
        void twoBlack() {
            var symbol = ManaSymbol.MonoColorHybrid.TWO_BLACK;
            assertThat(symbol.notation()).isEqualTo("{2/B}");
            assertThat(symbol.colorOption()).isEqualTo(ManaType.BLACK);
        }

        @Test
        void twoRed() {
            var symbol = ManaSymbol.MonoColorHybrid.TWO_RED;
            assertThat(symbol.notation()).isEqualTo("{2/R}");
            assertThat(symbol.colorOption()).isEqualTo(ManaType.RED);
        }

        @Test
        void twoGreen() {
            var symbol = ManaSymbol.MonoColorHybrid.TWO_GREEN;
            assertThat(symbol.notation()).isEqualTo("{2/G}");
            assertThat(symbol.colorOption()).isEqualTo(ManaType.GREEN);
        }
    }

    @Nested
    class ColorlessHybridSymbol {

        @Test
        void colorlessWhite() {
            var symbol = ManaSymbol.ColorlessHybrid.COLORLESS_WHITE;
            assertThat(symbol.manaValue()).isEqualTo(1);
            assertThat(symbol.notation()).isEqualTo("{C/W}");
            assertThat(symbol.colorOption()).isEqualTo(ManaType.WHITE);
            assertThat(symbol.colors()).isEqualTo(Colors.of(Color.WHITE));
        }

        @Test
        void colorlessBlue() {
            var symbol = ManaSymbol.ColorlessHybrid.COLORLESS_BLUE;
            assertThat(symbol.notation()).isEqualTo("{C/U}");
            assertThat(symbol.colorOption()).isEqualTo(ManaType.BLUE);
        }

        @Test
        void colorlessBlack() {
            var symbol = ManaSymbol.ColorlessHybrid.COLORLESS_BLACK;
            assertThat(symbol.notation()).isEqualTo("{C/B}");
            assertThat(symbol.colorOption()).isEqualTo(ManaType.BLACK);
        }

        @Test
        void colorlessRed() {
            var symbol = ManaSymbol.ColorlessHybrid.COLORLESS_RED;
            assertThat(symbol.notation()).isEqualTo("{C/R}");
            assertThat(symbol.colorOption()).isEqualTo(ManaType.RED);
        }

        @Test
        void colorlessGreen() {
            var symbol = ManaSymbol.ColorlessHybrid.COLORLESS_GREEN;
            assertThat(symbol.notation()).isEqualTo("{C/G}");
            assertThat(symbol.colorOption()).isEqualTo(ManaType.GREEN);
        }
    }
}
