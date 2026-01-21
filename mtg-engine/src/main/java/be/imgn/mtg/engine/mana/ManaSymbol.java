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
        W(ManaType.WHITE, Color.WHITE),
        U(ManaType.BLUE, Color.BLUE),
        B(ManaType.BLACK, Color.BLACK),
        R(ManaType.RED, Color.RED),
        G(ManaType.GREEN, Color.GREEN);

        private final ManaType manaType;
        private final Color color;

        Colored(ManaType manaType, Color color) {
            this.manaType = manaType;
            this.color = color;
        }

        /// Returns the mana type that can pay this symbol.
        public ManaType manaType() {
            return manaType;
        }

        /// Returns the color of this symbol.
        public Color color() {
            return color;
        }

        @Override
        public int manaValue() {
            return 1;
        }

        @Override
        public Colors colors() {
            return Colors.of(color);
        }

        @Override
        public String notation() {
            return "{" + name() + "}";
        }

        /// Returns the colored symbol for the given colored mana type.
        public static Colored fromManaType(ColoredManaType type) {
            return switch (type) {
                case WHITE -> W;
                case BLUE -> U;
                case BLACK -> B;
                case RED -> R;
                case GREEN -> G;
            };
        }
    }

    /// Colorless mana symbol: {C} ({@mtg.rule 107.4b}).
    ///
    /// Represents colorless mana specifically required in a cost.
    /// Can only be paid with colorless mana.
    enum Colorless implements ManaSymbol {
        INSTANCE;

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
        INSTANCE;

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
        W_P(Colored.W),
        U_P(Colored.U),
        B_P(Colored.B),
        R_P(Colored.R),
        G_P(Colored.G);

        private final Colored coloredSymbol;

        Phyrexian(Colored coloredSymbol) {
            this.coloredSymbol = coloredSymbol;
        }

        /// Returns the colored symbol that can pay this Phyrexian mana.
        public Colored coloredSymbol() {
            return coloredSymbol;
        }

        /// Returns the mana type that can pay this symbol.
        public ManaType manaType() {
            return coloredSymbol.manaType();
        }

        @Override
        public int manaValue() {
            return 1;
        }

        @Override
        public Colors colors() {
            return coloredSymbol.colors();
        }

        @Override
        public String notation() {
            return "{" + coloredSymbol.name() + "/P}";
        }
    }

    /// Hybrid Phyrexian mana symbols: {W/U/P}, {W/B/P}, etc. ({@mtg.rule 107.4g}).
    ///
    /// Can be paid with either of two colors of mana or by paying 2 life.
    enum HybridPhyrexian implements ManaSymbol {
        WU_P(Colored.W, Colored.U),
        WB_P(Colored.W, Colored.B),
        UB_P(Colored.U, Colored.B),
        UR_P(Colored.U, Colored.R),
        BR_P(Colored.B, Colored.R),
        BG_P(Colored.B, Colored.G),
        RG_P(Colored.R, Colored.G),
        RW_P(Colored.R, Colored.W),
        GW_P(Colored.G, Colored.W),
        GU_P(Colored.G, Colored.U);

        private final Colored option1;
        private final Colored option2;

        HybridPhyrexian(Colored option1, Colored option2) {
            this.option1 = option1;
            this.option2 = option2;
        }

        /// Returns the first color option.
        public Colored option1() {
            return option1;
        }

        /// Returns the second color option.
        public Colored option2() {
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
            return "{" + option1.name() + "/" + option2.name() + "/P}";
        }
    }

    /// Hybrid mana symbols ({@mtg.rule 107.4e}).
    ///
    /// Includes two-color hybrid ({W/U}), mono-color hybrid ({2/W}),
    /// and colorless hybrid ({C/W}).
    enum Hybrid implements ManaSymbol {
        // Two-color hybrid
        WU(Colored.W, Colored.U),
        WB(Colored.W, Colored.B),
        UB(Colored.U, Colored.B),
        UR(Colored.U, Colored.R),
        BR(Colored.B, Colored.R),
        BG(Colored.B, Colored.G),
        RG(Colored.R, Colored.G),
        RW(Colored.R, Colored.W),
        GW(Colored.G, Colored.W),
        GU(Colored.G, Colored.U),
        // Mono-color hybrid (can pay with color or 2 generic)
        TWO_W(Colored.W, HybridType.MONO),
        TWO_U(Colored.U, HybridType.MONO),
        TWO_B(Colored.B, HybridType.MONO),
        TWO_R(Colored.R, HybridType.MONO),
        TWO_G(Colored.G, HybridType.MONO),
        // Colorless hybrid (can pay with colorless or color)
        CW(Colored.W, HybridType.COLORLESS),
        CU(Colored.U, HybridType.COLORLESS),
        CB(Colored.B, HybridType.COLORLESS),
        CR(Colored.R, HybridType.COLORLESS),
        CG(Colored.G, HybridType.COLORLESS);

        private enum HybridType {
            TWO_COLOR,
            MONO,
            COLORLESS
        }

        private final Colored option1;
        private final Colored option2;
        private final HybridType hybridType;

        // Two-color constructor
        Hybrid(Colored option1, Colored option2) {
            this.option1 = option1;
            this.option2 = option2;
            this.hybridType = HybridType.TWO_COLOR;
        }

        // Mono/colorless constructor
        Hybrid(Colored option1, HybridType hybridType) {
            this.option1 = option1;
            this.option2 = option1; // Self-reference as placeholder
            this.hybridType = hybridType;
        }

        /// Returns the first color option.
        public Colored option1() {
            return option1;
        }

        /// Returns the second color option (same as option1 for mono-hybrid and colorless-hybrid).
        public Colored option2() {
            return option2;
        }

        /// Returns true if this is a mono-color hybrid ({2/W} style).
        public boolean isMonoHybrid() {
            return hybridType == HybridType.MONO;
        }

        /// Returns true if this is a colorless hybrid ({C/W} style).
        public boolean isColorlessHybrid() {
            return hybridType == HybridType.COLORLESS;
        }

        /// Returns true if this is a two-color hybrid ({W/U} style).
        public boolean isTwoColorHybrid() {
            return hybridType == HybridType.TWO_COLOR;
        }

        @Override
        public int manaValue() {
            return isMonoHybrid() ? 2 : 1;
        }

        @Override
        public Colors colors() {
            if (isTwoColorHybrid()) {
                return Colors.of(option1.color(), option2.color());
            }
            return Colors.of(option1.color());
        }

        @Override
        public String notation() {
            if (isMonoHybrid()) {
                return "{2/" + option1.name() + "}";
            }
            if (isColorlessHybrid()) {
                return "{C/" + option1.name() + "}";
            }
            return "{" + option1.name() + "/" + option2.name() + "}";
        }
    }
}
