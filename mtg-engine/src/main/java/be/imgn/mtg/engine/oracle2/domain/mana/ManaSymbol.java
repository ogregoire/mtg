package be.imgn.mtg.engine.oracle2.domain.mana;

import java.util.EnumSet;
import java.util.Set;

/// A mana symbol that appears in a mana cost ({@mtg.rule 107.4}).
///
/// Ten permitted shapes match the rule-107.4 catalog:
///
/// - [Colored] — `{W}`, `{U}`, `{B}`, `{R}`, `{G}` ({@mtg.rule 107.4a})
/// - [Colorless] — `{C}` ({@mtg.rule 107.4c})
/// - [Generic] — `{0}`, `{1}`, `{2}`, … ({@mtg.rule 107.4b})
/// - [Variable] — `{X}` ({@mtg.rule 107.4b})
/// - [Hybrid] — `{W/U}` … `{G/U}` ({@mtg.rule 107.4e})
/// - [MonoColorHybrid] — `{2/W}` … `{2/G}` ({@mtg.rule 107.4e})
/// - [ColorlessHybrid] — `{C/W}` … `{C/G}` ({@mtg.rule 107.4e})
/// - [Phyrexian] — `{W/P}` … `{G/P}` ({@mtg.rule 107.4f})
/// - [HybridPhyrexian] — `{W/U/P}` … `{G/U/P}` ({@mtg.rule 107.4f})
/// - [Snow] — `{S}` ({@mtg.rule 107.4h})
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

    /// All six [ManaType]s — the universal acceptance set used by
    /// generic, variable, and snow symbols.
    Set<ManaType> ALL_TYPES = Set.copyOf(EnumSet.allOf(ManaType.class));

    /// The bracketed oracle notation for this symbol, e.g. `{W}`,
    /// `{2/W}`, `{X}`, `{0}`.
    ///
    /// @return the notation
    String notation();

    /// The [ManaType]s of mana that can directly pay this symbol.
    /// Non-mana payment options (2 life for Phyrexian, "2 mana of any
    /// type" for [MonoColorHybrid], the snow-source constraint for
    /// [Snow]) are documented on each variant but not encoded in this
    /// set.
    ///
    /// @return an unmodifiable set of accepted mana types
    Set<ManaType> accepts();

    /// Colored mana symbols ({@mtg.rule 107.4a}). One per color,
    /// payable with one mana of that color.
    enum Colored implements ManaSymbol {
        /// `{W}`.
        WHITE(ManaType.WHITE),
        /// `{U}`.
        BLUE(ManaType.BLUE),
        /// `{B}`.
        BLACK(ManaType.BLACK),
        /// `{R}`.
        RED(ManaType.RED),
        /// `{G}`.
        GREEN(ManaType.GREEN);

        private final ManaType type;
        private final Set<ManaType> accepts;
        private final String notation;

        Colored(ManaType type) {
            this.type = type;
            this.accepts = Set.of(type);
            this.notation = type.notation();
        }

        /// The colored mana type that pays this symbol.
        ///
        /// @return the mana type
        public ManaType manaType() {
            return type;
        }

        @Override
        public Set<ManaType> accepts() {
            return accepts;
        }

        @Override
        public String notation() {
            return notation;
        }

        /// The [Colored] symbol matching the given mana type.
        ///
        /// @param type a colored mana type
        /// @return the matching symbol
        /// @throws IllegalArgumentException if `type` is [ManaType#COLORLESS]
        public static Colored of(ManaType type) {
            return switch (type) {
                case WHITE -> WHITE;
                case BLUE -> BLUE;
                case BLACK -> BLACK;
                case RED -> RED;
                case GREEN -> GREEN;
                case COLORLESS ->
                    throw new IllegalArgumentException("no colored symbol for COLORLESS — use Colorless.COLORLESS");
            };
        }
    }

    /// The colorless mana symbol `{C}` ({@mtg.rule 107.4c}). Payable
    /// only with colorless mana.
    enum Colorless implements ManaSymbol {
        /// The singleton `{C}`.
        COLORLESS;

        @Override
        public Set<ManaType> accepts() {
            return Set.of(ManaType.COLORLESS);
        }

        @Override
        public String notation() {
            return "{C}";
        }
    }

    /// Generic mana symbols `{0}`, `{1}`, `{2}`, … ({@mtg.rule 107.4b},
    /// {@mtg.rule 107.4d}). Payable with any type of mana.
    ///
    /// @param amount the generic mana amount; must be non-negative
    record Generic(int amount) implements ManaSymbol {
        public Generic {
            if (amount < 0) {
                throw new IllegalArgumentException("generic mana amount cannot be negative: " + amount);
            }
        }

        @Override
        public Set<ManaType> accepts() {
            return ALL_TYPES;
        }

        @Override
        public String notation() {
            return "{" + amount + "}";
        }
    }

    /// The variable mana symbol `{X}` ({@mtg.rule 107.4b}). Resolved to
    /// a specific generic amount when the spell or ability is
    /// announced.
    enum Variable implements ManaSymbol {
        /// The singleton `{X}`.
        X;

        @Override
        public Set<ManaType> accepts() {
            return ALL_TYPES;
        }

        @Override
        public String notation() {
            return "{X}";
        }
    }

    /// Two-color hybrid symbols ({@mtg.rule 107.4e}). Payable with one
    /// mana of either component color. Enumeration follows the rule
    /// 107.4 order: each color paired with the next two in the WUBRG
    /// cycle.
    enum Hybrid implements ManaSymbol {
        /// `{W/U}`.
        WHITE_BLUE(ManaType.WHITE, ManaType.BLUE),
        /// `{W/B}`.
        WHITE_BLACK(ManaType.WHITE, ManaType.BLACK),
        /// `{U/B}`.
        BLUE_BLACK(ManaType.BLUE, ManaType.BLACK),
        /// `{U/R}`.
        BLUE_RED(ManaType.BLUE, ManaType.RED),
        /// `{B/R}`.
        BLACK_RED(ManaType.BLACK, ManaType.RED),
        /// `{B/G}`.
        BLACK_GREEN(ManaType.BLACK, ManaType.GREEN),
        /// `{R/G}`.
        RED_GREEN(ManaType.RED, ManaType.GREEN),
        /// `{R/W}`.
        RED_WHITE(ManaType.RED, ManaType.WHITE),
        /// `{G/W}`.
        GREEN_WHITE(ManaType.GREEN, ManaType.WHITE),
        /// `{G/U}`.
        GREEN_BLUE(ManaType.GREEN, ManaType.BLUE);

        private final ManaType first;
        private final ManaType second;
        private final Set<ManaType> accepts;
        private final String notation;

        Hybrid(ManaType first, ManaType second) {
            this.first = first;
            this.second = second;
            this.accepts = Set.of(first, second);
            this.notation = "{" + letter(first) + "/" + letter(second) + "}";
        }

        /// The first colored option.
        ///
        /// @return the first mana type
        public ManaType first() {
            return first;
        }

        /// The second colored option.
        ///
        /// @return the second mana type
        public ManaType second() {
            return second;
        }

        @Override
        public Set<ManaType> accepts() {
            return accepts;
        }

        @Override
        public String notation() {
            return notation;
        }

        /// The [Hybrid] symbol matching the given pair, in oracle-text
        /// order.
        ///
        /// @param first the first mana type
        /// @param second the second mana type
        /// @return the matching symbol
        /// @throws IllegalArgumentException if `(first, second)` is not
        ///   one of the ten rule-107.4 pairings
        public static Hybrid of(ManaType first, ManaType second) {
            for (var h : values()) {
                if (h.first == first && h.second == second) return h;
            }
            throw new IllegalArgumentException("not a valid hybrid pair: " + first + "/" + second);
        }
    }

    /// Mono-color hybrid symbols `{2/W}`, `{2/U}`, … ({@mtg.rule 107.4e}).
    /// Payable with one mana of the listed color OR with two mana of
    /// any type. The "2 mana of any type" option is not encoded in
    /// [#accepts]; only the colored option is.
    enum MonoColorHybrid implements ManaSymbol {
        /// `{2/W}`.
        TWO_WHITE(ManaType.WHITE),
        /// `{2/U}`.
        TWO_BLUE(ManaType.BLUE),
        /// `{2/B}`.
        TWO_BLACK(ManaType.BLACK),
        /// `{2/R}`.
        TWO_RED(ManaType.RED),
        /// `{2/G}`.
        TWO_GREEN(ManaType.GREEN);

        private final ManaType type;
        private final Set<ManaType> accepts;
        private final String notation;

        MonoColorHybrid(ManaType type) {
            this.type = type;
            this.accepts = Set.of(type);
            this.notation = "{2/" + letter(type) + "}";
        }

        /// The colored option for this symbol.
        ///
        /// @return the mana type
        public ManaType manaType() {
            return type;
        }

        @Override
        public Set<ManaType> accepts() {
            return accepts;
        }

        @Override
        public String notation() {
            return notation;
        }

        /// The [MonoColorHybrid] symbol matching the given mana type.
        ///
        /// @param type a colored mana type
        /// @return the matching symbol
        /// @throws IllegalArgumentException if `type` is [ManaType#COLORLESS]
        public static MonoColorHybrid of(ManaType type) {
            return switch (type) {
                case WHITE -> TWO_WHITE;
                case BLUE -> TWO_BLUE;
                case BLACK -> TWO_BLACK;
                case RED -> TWO_RED;
                case GREEN -> TWO_GREEN;
                case COLORLESS -> throw new IllegalArgumentException("no mono-color hybrid for COLORLESS");
            };
        }
    }

    /// Colorless hybrid symbols `{C/W}`, `{C/U}`, … ({@mtg.rule 107.4e}).
    /// Payable with one colorless mana OR with one mana of the listed
    /// color.
    enum ColorlessHybrid implements ManaSymbol {
        /// `{C/W}`.
        COLORLESS_WHITE(ManaType.WHITE),
        /// `{C/U}`.
        COLORLESS_BLUE(ManaType.BLUE),
        /// `{C/B}`.
        COLORLESS_BLACK(ManaType.BLACK),
        /// `{C/R}`.
        COLORLESS_RED(ManaType.RED),
        /// `{C/G}`.
        COLORLESS_GREEN(ManaType.GREEN);

        private final ManaType type;
        private final Set<ManaType> accepts;
        private final String notation;

        ColorlessHybrid(ManaType type) {
            this.type = type;
            this.accepts = Set.of(type, ManaType.COLORLESS);
            this.notation = "{C/" + letter(type) + "}";
        }

        /// The colored option for this symbol.
        ///
        /// @return the mana type
        public ManaType manaType() {
            return type;
        }

        @Override
        public Set<ManaType> accepts() {
            return accepts;
        }

        @Override
        public String notation() {
            return notation;
        }

        /// The [ColorlessHybrid] symbol matching the given mana type.
        ///
        /// @param type a colored mana type
        /// @return the matching symbol
        /// @throws IllegalArgumentException if `type` is [ManaType#COLORLESS]
        public static ColorlessHybrid of(ManaType type) {
            return switch (type) {
                case WHITE -> COLORLESS_WHITE;
                case BLUE -> COLORLESS_BLUE;
                case BLACK -> COLORLESS_BLACK;
                case RED -> COLORLESS_RED;
                case GREEN -> COLORLESS_GREEN;
                case COLORLESS -> throw new IllegalArgumentException("no colorless hybrid for COLORLESS");
            };
        }
    }

    /// Phyrexian mana symbols `{W/P}`, `{U/P}`, … ({@mtg.rule 107.4f}).
    /// Payable with one mana of the listed color OR by paying 2 life.
    /// The 2-life option is not encoded in [#accepts].
    enum Phyrexian implements ManaSymbol {
        /// `{W/P}`.
        WHITE(ManaType.WHITE),
        /// `{U/P}`.
        BLUE(ManaType.BLUE),
        /// `{B/P}`.
        BLACK(ManaType.BLACK),
        /// `{R/P}`.
        RED(ManaType.RED),
        /// `{G/P}`.
        GREEN(ManaType.GREEN);

        private final ManaType type;
        private final Set<ManaType> accepts;
        private final String notation;

        Phyrexian(ManaType type) {
            this.type = type;
            this.accepts = Set.of(type);
            this.notation = "{" + letter(type) + "/P}";
        }

        /// The colored option for this symbol.
        ///
        /// @return the mana type
        public ManaType manaType() {
            return type;
        }

        @Override
        public Set<ManaType> accepts() {
            return accepts;
        }

        @Override
        public String notation() {
            return notation;
        }

        /// The [Phyrexian] symbol matching the given mana type.
        ///
        /// @param type a colored mana type
        /// @return the matching symbol
        /// @throws IllegalArgumentException if `type` is [ManaType#COLORLESS]
        public static Phyrexian of(ManaType type) {
            return switch (type) {
                case WHITE -> WHITE;
                case BLUE -> BLUE;
                case BLACK -> BLACK;
                case RED -> RED;
                case GREEN -> GREEN;
                case COLORLESS -> throw new IllegalArgumentException("no Phyrexian symbol for COLORLESS");
            };
        }
    }

    /// Hybrid Phyrexian symbols `{W/U/P}`, … ({@mtg.rule 107.4f}).
    /// Payable with one mana of either component color OR by paying 2
    /// life. The 2-life option is not encoded in [#accepts].
    enum HybridPhyrexian implements ManaSymbol {
        /// `{W/U/P}`.
        WHITE_BLUE(ManaType.WHITE, ManaType.BLUE),
        /// `{W/B/P}`.
        WHITE_BLACK(ManaType.WHITE, ManaType.BLACK),
        /// `{U/B/P}`.
        BLUE_BLACK(ManaType.BLUE, ManaType.BLACK),
        /// `{U/R/P}`.
        BLUE_RED(ManaType.BLUE, ManaType.RED),
        /// `{B/R/P}`.
        BLACK_RED(ManaType.BLACK, ManaType.RED),
        /// `{B/G/P}`.
        BLACK_GREEN(ManaType.BLACK, ManaType.GREEN),
        /// `{R/G/P}`.
        RED_GREEN(ManaType.RED, ManaType.GREEN),
        /// `{R/W/P}`.
        RED_WHITE(ManaType.RED, ManaType.WHITE),
        /// `{G/W/P}`.
        GREEN_WHITE(ManaType.GREEN, ManaType.WHITE),
        /// `{G/U/P}`.
        GREEN_BLUE(ManaType.GREEN, ManaType.BLUE);

        private final ManaType first;
        private final ManaType second;
        private final Set<ManaType> accepts;
        private final String notation;

        HybridPhyrexian(ManaType first, ManaType second) {
            this.first = first;
            this.second = second;
            this.accepts = Set.of(first, second);
            this.notation = "{" + letter(first) + "/" + letter(second) + "/P}";
        }

        /// The first colored option.
        ///
        /// @return the first mana type
        public ManaType first() {
            return first;
        }

        /// The second colored option.
        ///
        /// @return the second mana type
        public ManaType second() {
            return second;
        }

        @Override
        public Set<ManaType> accepts() {
            return accepts;
        }

        @Override
        public String notation() {
            return notation;
        }

        /// The [HybridPhyrexian] symbol matching the given pair, in
        /// oracle-text order.
        ///
        /// @param first the first mana type
        /// @param second the second mana type
        /// @return the matching symbol
        /// @throws IllegalArgumentException if `(first, second)` is not
        ///   one of the ten rule-107.4f pairings
        public static HybridPhyrexian of(ManaType first, ManaType second) {
            for (var h : values()) {
                if (h.first == first && h.second == second) return h;
            }
            throw new IllegalArgumentException("not a valid hybrid Phyrexian pair: " + first + "/" + second);
        }
    }

    /// The snow mana symbol `{S}` ({@mtg.rule 107.4h}). Payable with
    /// one mana of any type produced by a snow source. Snow is neither
    /// a color nor a type of mana — the "snow source" constraint is
    /// orthogonal and not encoded in [#accepts].
    enum Snow implements ManaSymbol {
        /// The singleton `{S}`.
        SNOW;

        @Override
        public Set<ManaType> accepts() {
            return ALL_TYPES;
        }

        @Override
        public String notation() {
            return "{S}";
        }
    }

    /// The single-letter form of a mana type, e.g. `W` for white.
    /// Used by the symbol enums to compose their notations.
    private static String letter(ManaType type) {
        var n = type.notation();
        return n.substring(1, n.length() - 1);
    }
}
