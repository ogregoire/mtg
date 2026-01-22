package be.imgn.mtg.engine.mana;

import java.util.List;

import be.imgn.mtg.engine.characteristics.Color;

/// The types of mana in Magic: The Gathering ({@mtg.rule 106.1}).
///
/// There are six types of mana: white, blue, black, red, green, and colorless.
/// The first five correspond to the five colors of Magic.
public sealed interface ManaType permits ManaType.Colored, ManaType.Colorless {

    /// White mana, produced by Plains and white sources.
    Colored WHITE = Colored.WHITE;
    /// Blue mana, produced by Islands and blue sources.
    Colored BLUE = Colored.BLUE;
    /// Black mana, produced by Swamps and black sources.
    Colored BLACK = Colored.BLACK;
    /// Red mana, produced by Mountains and red sources.
    Colored RED = Colored.RED;
    /// Green mana, produced by Forests and green sources.
    Colored GREEN = Colored.GREEN;
    /// Colorless mana, produced by various sources.
    /// Colorless is not a color ({@mtg.rule 105.4}).
    Colorless COLORLESS = Colorless.COLORLESS;

    /// All mana types in WUBRG order followed by colorless.
    List<ManaType> ALL = List.of(WHITE, BLUE, BLACK, RED, GREEN, COLORLESS);

    /// Returns the unique name for this mana type.
    ///
    /// @return the name (WHITE, BLUE, BLACK, RED, GREEN, or COLORLESS)
    String name();

    /// Returns the notation for this mana type (e.g., "{W}", "{C}").
    ///
    /// @return the mana symbol notation
    String notation();

    /// Returns all mana type values.
    ///
    /// @return all six mana types
    static List<ManaType> values() {
        return ALL;
    }

    /// Returns the mana type corresponding to the given color.
    ///
    /// @param color the color
    /// @return the corresponding mana type
    static Colored fromColor(Color color) {
        return switch (color) {
            case WHITE -> Colored.WHITE;
            case BLUE -> Colored.BLUE;
            case BLACK -> Colored.BLACK;
            case RED -> Colored.RED;
            case GREEN -> Colored.GREEN;
        };
    }

    /// Colored mana types: white, blue, black, red, and green.
    ///
    /// Each colored mana type corresponds to one of the five colors of Magic.
    enum Colored implements ManaType {
        /// White mana, produced by Plains and white sources.
        WHITE(Color.WHITE, "{W}"),
        /// Blue mana, produced by Islands and blue sources.
        BLUE(Color.BLUE, "{U}"),
        /// Black mana, produced by Swamps and black sources.
        BLACK(Color.BLACK, "{B}"),
        /// Red mana, produced by Mountains and red sources.
        RED(Color.RED, "{R}"),
        /// Green mana, produced by Forests and green sources.
        GREEN(Color.GREEN, "{G}");

        private final Color color;
        private final String notation;

        Colored(Color color, String notation) {
            this.color = color;
            this.notation = notation;
        }

        /// Returns the color associated with this mana type.
        ///
        /// @return the color
        public Color color() {
            return color;
        }

        @Override
        public String notation() {
            return notation;
        }
    }

    /// Colorless mana type.
    ///
    /// Colorless is not a color ({@mtg.rule 105.4}).
    enum Colorless implements ManaType {
        /// Colorless mana, produced by various sources.
        COLORLESS;

        @Override
        public String notation() {
            return "{C}";
        }
    }
}
