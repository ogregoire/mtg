package be.imgn.mtg.engine.mana;

import java.util.Optional;

import be.imgn.mtg.engine.characteristics.Color;

/// Colored mana types: white, blue, black, red, and green.
///
/// Each colored mana type corresponds to one of the five colors of Magic.
public enum ColoredManaType implements ManaType {
    /// White mana, produced by Plains and white sources.
    WHITE(Color.WHITE),
    /// Blue mana, produced by Islands and blue sources.
    BLUE(Color.BLUE),
    /// Black mana, produced by Swamps and black sources.
    BLACK(Color.BLACK),
    /// Red mana, produced by Mountains and red sources.
    RED(Color.RED),
    /// Green mana, produced by Forests and green sources.
    GREEN(Color.GREEN);

    private final Color color;

    ColoredManaType(Color color) {
        this.color = color;
    }

    @Override
    public Optional<Color> color() {
        return Optional.of(color);
    }

    @Override
    public boolean isColored() {
        return true;
    }

    /// Returns the color associated with this mana type.
    ///
    /// @return the color (never null for colored mana)
    public Color colorValue() {
        return color;
    }
}
