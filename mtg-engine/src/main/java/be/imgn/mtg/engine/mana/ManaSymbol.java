package be.imgn.mtg.engine.mana;

import be.imgn.mtg.engine.characteristics.Color;
import be.imgn.mtg.engine.characteristics.Colors;

/// A mana symbol in a mana cost ({@mtg.rule 107.4}).
///
/// Mana symbols represent the mana that must be paid as part of a cost.
/// There are many types of mana symbols: colored, colorless, generic,
/// variable (X), hybrid, Phyrexian, hybrid Phyrexian, and snow.
public sealed interface ManaSymbol
        permits ManaSymbol.Colored,
                ManaSymbol.Colorless,
                ManaSymbol.Generic,
                ManaSymbol.Variable,
                ManaSymbol.Hybrid,
                ManaSymbol.MonoColorHybrid,
                ManaSymbol.ColorlessHybrid,
                ManaSymbol.Phyrexian,
                ManaSymbol.HybridPhyrexian,
                ManaSymbol.Snow {

    /// Returns the mana value of this symbol ({@mtg.rule 202.3}).
    ///
    /// @return the mana value
    int manaValue();

    /// Returns the colors of this symbol.
    ///
    /// @return the colors (empty for colorless/generic)
    Colors colors();

    /// Returns the notation for this symbol (e.g., "{W}", "{2}", "{W/U}").
    ///
    /// @return the notation string
    String notation();

    /// Colored mana symbols: {W}, {U}, {B}, {R}, {G} ({@mtg.rule 107.4a}).
    enum Colored implements ManaSymbol {
        /// White mana symbol {W}.
        WHITE(ManaType.WHITE, "{W}"),
        /// Blue mana symbol {U}.
        BLUE(ManaType.BLUE, "{U}"),
        /// Black mana symbol {B}.
        BLACK(ManaType.BLACK, "{B}"),
        /// Red mana symbol {R}.
        RED(ManaType.RED, "{R}"),
        /// Green mana symbol {G}.
        GREEN(ManaType.GREEN, "{G}");

        private final ManaType.Colored manaType;
        private final String notation;

        Colored(ManaType.Colored manaType, String notation) {
            this.manaType = manaType;
            this.notation = notation;
        }

        /// Returns the mana type that can pay this symbol.
        ///
        /// @return the colored mana type
        public ManaType.Colored manaType() {
            return manaType;
        }

        /// Returns the color of this symbol.
        ///
        /// @return the color
        public Color color() {
            return manaType.color();
        }

        @Override
        public int manaValue() {
            return 1;
        }

        @Override
        public Colors colors() {
            return Colors.of(manaType.color());
        }

        @Override
        public String notation() {
            return notation;
        }

        /// Returns the colored symbol for the given colored mana type.
        ///
        /// @param type the colored mana type
        /// @return the corresponding colored mana symbol
        public static Colored fromManaType(ManaType.Colored type) {
            return switch (type) {
                case WHITE -> WHITE;
                case BLUE -> BLUE;
                case BLACK -> BLACK;
                case RED -> RED;
                case GREEN -> GREEN;
            };
        }
    }

    /// Colorless mana symbol: {C} ({@mtg.rule 107.4b}).
    ///
    /// Represents colorless mana specifically required in a cost.
    /// Can only be paid with colorless mana.
    enum Colorless implements ManaSymbol {
        /// The singleton colorless mana symbol {C}.
        COLORLESS;

        @Override
        public int manaValue() {
            return 1;
        }

        @Override
        public Colors colors() {
            return Colors.empty();
        }

        @Override
        public String notation() {
            return "{C}";
        }
    }

    /// Generic mana symbol: {0}, {1}, {2}, etc. ({@mtg.rule 107.4a}).
    ///
    /// Generic mana can be paid with any type of mana.
    ///
    /// @param amount the amount of generic mana
    record Generic(int amount) implements ManaSymbol {
        /// Creates a generic mana symbol.
        public Generic {
            if (amount < 0) {
                throw new IllegalArgumentException("Generic mana amount cannot be negative: " + amount);
            }
        }

        @Override
        public int manaValue() {
            return amount;
        }

        @Override
        public Colors colors() {
            return Colors.empty();
        }

        @Override
        public String notation() {
            return "{" + amount + "}";
        }
    }

    /// Variable mana symbol: {X} ({@mtg.rule 107.4a}).
    ///
    /// X is a variable that is determined when the spell is cast.
    enum Variable implements ManaSymbol {
        /// The variable mana symbol {X}.
        X;

        @Override
        public int manaValue() {
            return 0; // X has mana value 0 except on the stack
        }

        @Override
        public Colors colors() {
            return Colors.empty();
        }

        @Override
        public String notation() {
            return "{X}";
        }
    }

    /// Snow mana symbol: {S} ({@mtg.rule 107.4h}).
    ///
    /// Can be paid with any mana produced by a snow source.
    enum Snow implements ManaSymbol {
        /// The singleton snow mana symbol {S}.
        SNOW;

        @Override
        public int manaValue() {
            return 1;
        }

        @Override
        public Colors colors() {
            return Colors.empty();
        }

        @Override
        public String notation() {
            return "{S}";
        }
    }

    /// Phyrexian mana symbols: {W/P}, {U/P}, {B/P}, {R/P}, {G/P} ({@mtg.rule 107.4f}).
    ///
    /// Can be paid with the appropriate colored mana or by paying 2 life.
    enum Phyrexian implements ManaSymbol {
        /// White Phyrexian mana symbol {W/P}.
        WHITE_PHYREXIAN(ManaType.WHITE, "{W/P}"),
        /// Blue Phyrexian mana symbol {U/P}.
        BLUE_PHYREXIAN(ManaType.BLUE, "{U/P}"),
        /// Black Phyrexian mana symbol {B/P}.
        BLACK_PHYREXIAN(ManaType.BLACK, "{B/P}"),
        /// Red Phyrexian mana symbol {R/P}.
        RED_PHYREXIAN(ManaType.RED, "{R/P}"),
        /// Green Phyrexian mana symbol {G/P}.
        GREEN_PHYREXIAN(ManaType.GREEN, "{G/P}");

        private final ManaType.Colored manaType;
        private final String notation;

        Phyrexian(ManaType.Colored manaType, String notation) {
            this.manaType = manaType;
            this.notation = notation;
        }

        /// Returns the mana type that can pay this symbol.
        ///
        /// @return the colored mana type
        public ManaType.Colored manaType() {
            return manaType;
        }

        @Override
        public int manaValue() {
            return 1;
        }

        @Override
        public Colors colors() {
            return Colors.of(manaType.color());
        }

        @Override
        public String notation() {
            return notation;
        }
    }

    /// Hybrid Phyrexian mana symbols: {W/U/P}, {W/B/P}, etc. ({@mtg.rule 107.4g}).
    ///
    /// Can be paid with either of two colors of mana or by paying 2 life.
    enum HybridPhyrexian implements ManaSymbol {
        /// White/blue Phyrexian hybrid {W/U/P}.
        WHITE_BLUE_PHYREXIAN(ManaType.WHITE, ManaType.BLUE, "{W/U/P}"),
        /// White/black Phyrexian hybrid {W/B/P}.
        WHITE_BLACK_PHYREXIAN(ManaType.WHITE, ManaType.BLACK, "{W/B/P}"),
        /// Blue/black Phyrexian hybrid {U/B/P}.
        BLUE_BLACK_PHYREXIAN(ManaType.BLUE, ManaType.BLACK, "{U/B/P}"),
        /// Blue/red Phyrexian hybrid {U/R/P}.
        BLUE_RED_PHYREXIAN(ManaType.BLUE, ManaType.RED, "{U/R/P}"),
        /// Black/red Phyrexian hybrid {B/R/P}.
        BLACK_RED_PHYREXIAN(ManaType.BLACK, ManaType.RED, "{B/R/P}"),
        /// Black/green Phyrexian hybrid {B/G/P}.
        BLACK_GREEN_PHYREXIAN(ManaType.BLACK, ManaType.GREEN, "{B/G/P}"),
        /// Red/green Phyrexian hybrid {R/G/P}.
        RED_GREEN_PHYREXIAN(ManaType.RED, ManaType.GREEN, "{R/G/P}"),
        /// Red/white Phyrexian hybrid {R/W/P}.
        RED_WHITE_PHYREXIAN(ManaType.RED, ManaType.WHITE, "{R/W/P}"),
        /// Green/white Phyrexian hybrid {G/W/P}.
        GREEN_WHITE_PHYREXIAN(ManaType.GREEN, ManaType.WHITE, "{G/W/P}"),
        /// Green/blue Phyrexian hybrid {G/U/P}.
        GREEN_BLUE_PHYREXIAN(ManaType.GREEN, ManaType.BLUE, "{G/U/P}");

        private final ManaType.Colored option1;
        private final ManaType.Colored option2;
        private final String notation;

        HybridPhyrexian(ManaType.Colored option1, ManaType.Colored option2, String notation) {
            this.option1 = option1;
            this.option2 = option2;
            this.notation = notation;
        }

        /// Returns the first mana type option.
        ///
        /// @return the first colored mana type
        public ManaType.Colored option1() {
            return option1;
        }

        /// Returns the second mana type option.
        ///
        /// @return the second colored mana type
        public ManaType.Colored option2() {
            return option2;
        }

        @Override
        public int manaValue() {
            return 1;
        }

        @Override
        public Colors colors() {
            return Colors.of(option1.color(), option2.color());
        }

        @Override
        public String notation() {
            return notation;
        }
    }

    /// Two-color hybrid mana symbols: {W/U}, {W/B}, etc. ({@mtg.rule 107.4e}).
    ///
    /// Can be paid with either of two colors of mana.
    enum Hybrid implements ManaSymbol {
        /// White/blue hybrid {W/U}.
        WHITE_BLUE(ManaType.WHITE, ManaType.BLUE, "{W/U}"),
        /// White/black hybrid {W/B}.
        WHITE_BLACK(ManaType.WHITE, ManaType.BLACK, "{W/B}"),
        /// Blue/black hybrid {U/B}.
        BLUE_BLACK(ManaType.BLUE, ManaType.BLACK, "{U/B}"),
        /// Blue/red hybrid {U/R}.
        BLUE_RED(ManaType.BLUE, ManaType.RED, "{U/R}"),
        /// Black/red hybrid {B/R}.
        BLACK_RED(ManaType.BLACK, ManaType.RED, "{B/R}"),
        /// Black/green hybrid {B/G}.
        BLACK_GREEN(ManaType.BLACK, ManaType.GREEN, "{B/G}"),
        /// Red/green hybrid {R/G}.
        RED_GREEN(ManaType.RED, ManaType.GREEN, "{R/G}"),
        /// Red/white hybrid {R/W}.
        RED_WHITE(ManaType.RED, ManaType.WHITE, "{R/W}"),
        /// Green/white hybrid {G/W}.
        GREEN_WHITE(ManaType.GREEN, ManaType.WHITE, "{G/W}"),
        /// Green/blue hybrid {G/U}.
        GREEN_BLUE(ManaType.GREEN, ManaType.BLUE, "{G/U}");

        private final ManaType.Colored option1;
        private final ManaType.Colored option2;
        private final String notation;

        Hybrid(ManaType.Colored option1, ManaType.Colored option2, String notation) {
            this.option1 = option1;
            this.option2 = option2;
            this.notation = notation;
        }

        /// Returns the first mana type option.
        ///
        /// @return the first colored mana type
        public ManaType.Colored option1() {
            return option1;
        }

        /// Returns the second mana type option.
        ///
        /// @return the second colored mana type
        public ManaType.Colored option2() {
            return option2;
        }

        @Override
        public int manaValue() {
            return 1;
        }

        @Override
        public Colors colors() {
            return Colors.of(option1.color(), option2.color());
        }

        @Override
        public String notation() {
            return notation;
        }
    }

    /// Mono-color hybrid mana symbols: {2/W}, {2/U}, etc. ({@mtg.rule 107.4e}).
    ///
    /// Can be paid with either one colored mana or two mana of any type.
    enum MonoColorHybrid implements ManaSymbol {
        /// Two-or-white hybrid {2/W}.
        TWO_WHITE(ManaType.WHITE, "{2/W}"),
        /// Two-or-blue hybrid {2/U}.
        TWO_BLUE(ManaType.BLUE, "{2/U}"),
        /// Two-or-black hybrid {2/B}.
        TWO_BLACK(ManaType.BLACK, "{2/B}"),
        /// Two-or-red hybrid {2/R}.
        TWO_RED(ManaType.RED, "{2/R}"),
        /// Two-or-green hybrid {2/G}.
        TWO_GREEN(ManaType.GREEN, "{2/G}");

        private final ManaType.Colored colorOption;
        private final String notation;

        MonoColorHybrid(ManaType.Colored colorOption, String notation) {
            this.colorOption = colorOption;
            this.notation = notation;
        }

        /// Returns the colored mana type option.
        ///
        /// @return the colored mana type
        public ManaType.Colored colorOption() {
            return colorOption;
        }

        @Override
        public int manaValue() {
            return 2;
        }

        @Override
        public Colors colors() {
            return Colors.of(colorOption.color());
        }

        @Override
        public String notation() {
            return notation;
        }
    }

    /// Colorless hybrid mana symbols: {C/W}, {C/U}, etc. ({@mtg.rule 107.4e}).
    ///
    /// Can be paid with either colorless mana or one colored mana.
    enum ColorlessHybrid implements ManaSymbol {
        /// Colorless-or-white hybrid {C/W}.
        COLORLESS_WHITE(ManaType.WHITE, "{C/W}"),
        /// Colorless-or-blue hybrid {C/U}.
        COLORLESS_BLUE(ManaType.BLUE, "{C/U}"),
        /// Colorless-or-black hybrid {C/B}.
        COLORLESS_BLACK(ManaType.BLACK, "{C/B}"),
        /// Colorless-or-red hybrid {C/R}.
        COLORLESS_RED(ManaType.RED, "{C/R}"),
        /// Colorless-or-green hybrid {C/G}.
        COLORLESS_GREEN(ManaType.GREEN, "{C/G}");

        private final ManaType.Colored colorOption;
        private final String notation;

        ColorlessHybrid(ManaType.Colored colorOption, String notation) {
            this.colorOption = colorOption;
            this.notation = notation;
        }

        /// Returns the colored mana type option.
        ///
        /// @return the colored mana type
        public ManaType.Colored colorOption() {
            return colorOption;
        }

        @Override
        public int manaValue() {
            return 1;
        }

        @Override
        public Colors colors() {
            return Colors.of(colorOption.color());
        }

        @Override
        public String notation() {
            return notation;
        }
    }
}
