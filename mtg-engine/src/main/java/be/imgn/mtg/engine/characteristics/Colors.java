package be.imgn.mtg.engine.characteristics;

import be.imgn.mtg.engine.characteristics.internal.Characteristics;
import be.imgn.mtg.engine.characteristics.internal.DefaultColors;

/// The color characteristic of a game object ({@mtg.rule 105}).
///
/// A card's color is determined by the mana symbols in its mana cost and/or its color indicator.
/// Objects without colored mana symbols and without a color indicator are colorless.
public interface Colors extends Characteristics<Color> {

    /// Returns an empty Colors collection.
    ///
    /// @return an empty collection
    static Colors empty() {
        return DefaultColors.empty();
    }

    /// Returns a Colors collection containing the specified colors.
    ///
    /// @param colors the colors to include
    /// @return a collection containing the colors
    static Colors of(Color... colors) {
        return DefaultColors.of(colors);
    }

    /// Returns a new builder for Colors.
    ///
    /// @return a new builder
    static Builder builder() {
        return DefaultColors.builder();
    }

    @Override
    Builder toBuilder();

    /// Returns true if this contains white.
    ///
    /// @return true if white is present
    default boolean isWhite() {
        return contains(Color.WHITE);
    }

    /// Returns true if this contains blue.
    ///
    /// @return true if blue is present
    default boolean isBlue() {
        return contains(Color.BLUE);
    }

    /// Returns true if this contains black.
    ///
    /// @return true if black is present
    default boolean isBlack() {
        return contains(Color.BLACK);
    }

    /// Returns true if this contains red.
    ///
    /// @return true if red is present
    default boolean isRed() {
        return contains(Color.RED);
    }

    /// Returns true if this contains green.
    ///
    /// @return true if green is present
    default boolean isGreen() {
        return contains(Color.GREEN);
    }

    /// Returns true if this contains at least one color.
    ///
    /// @return true if colored
    default boolean isColored() {
        return !isEmpty();
    }

    /// Returns true if this contains no colors.
    ///
    /// @return true if colorless
    default boolean isColorless() {
        return isEmpty();
    }

    /// Returns true if this contains exactly one color.
    ///
    /// @return true if mono-colored
    default boolean isMonoColored() {
        return count() == 1;
    }

    /// Returns true if this contains two or more colors.
    ///
    /// @return true if multi-colored
    default boolean isMultiColored() {
        return count() >= 2;
    }

    /// Returns true if this contains all five colors.
    ///
    /// @return true if all colors present
    default boolean isAllColors() {
        return count() == 5;
    }

    /// Builder for Colors.
    interface Builder extends Characteristics.Builder<Color, Colors, Builder> {}
}
