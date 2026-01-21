package be.imgn.mtg.engine.mana;

import java.util.List;
import java.util.Optional;

import be.imgn.mtg.engine.characteristics.Color;

/// The types of mana in Magic: The Gathering ({@mtg.rule 106.1}).
///
/// There are six types of mana: white, blue, black, red, green, and colorless.
/// The first five correspond to the five colors of Magic.
public sealed interface ManaType permits ColoredManaType, ColorlessManaType {

    /// White mana, produced by Plains and white sources.
    ColoredManaType WHITE = ColoredManaType.WHITE;
    /// Blue mana, produced by Islands and blue sources.
    ColoredManaType BLUE = ColoredManaType.BLUE;
    /// Black mana, produced by Swamps and black sources.
    ColoredManaType BLACK = ColoredManaType.BLACK;
    /// Red mana, produced by Mountains and red sources.
    ColoredManaType RED = ColoredManaType.RED;
    /// Green mana, produced by Forests and green sources.
    ColoredManaType GREEN = ColoredManaType.GREEN;
    /// Colorless mana, produced by various sources.
    /// Colorless is not a color ({@mtg.rule 105.4}).
    ColorlessManaType COLORLESS = ColorlessManaType.COLORLESS;

    /// All mana types in WUBRG order followed by colorless.
    List<ManaType> ALL = List.of(WHITE, BLUE, BLACK, RED, GREEN, COLORLESS);

    /// Returns the color associated with this mana type, if any.
    ///
    /// @return the color, or empty if this is colorless mana
    Optional<Color> color();

    /// Returns true if this is a colored mana type.
    ///
    /// @return true if WHITE, BLUE, BLACK, RED, or GREEN
    boolean isColored();

    /// Returns the unique name for this mana type.
    ///
    /// @return the name (WHITE, BLUE, BLACK, RED, GREEN, or COLORLESS)
    String name();

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
    static ColoredManaType fromColor(Color color) {
        return switch (color) {
            case WHITE -> ColoredManaType.WHITE;
            case BLUE -> ColoredManaType.BLUE;
            case BLACK -> ColoredManaType.BLACK;
            case RED -> ColoredManaType.RED;
            case GREEN -> ColoredManaType.GREEN;
        };
    }
}
