package be.imgn.mtg.engine.characteristics;

import be.imgn.mtg.engine.characteristics.internal.Characteristics;
import be.imgn.mtg.engine.characteristics.internal.DefaultColors;

/// The color characteristic of a game object ({@mtg.rule 105}).
///
/// A card's color is determined by the mana symbols in its mana cost and/or its color indicator.
/// Objects without colored mana symbols and without a color indicator are colorless.
public interface Colors extends Characteristics<Color> {

    /// Returns an empty Colors collection.
    static Colors empty() {
        return DefaultColors.empty();
    }

    /// Returns a Colors collection containing the specified colors.
    static Colors of(Color... colors) {
        return DefaultColors.of(colors);
    }

    /// Returns a new builder for Colors.
    static Builder builder() {
        return DefaultColors.builder();
    }

    @Override
    Builder toBuilder();

    /// Returns true if this contains white.
    default boolean isWhite() {
        return contains(Color.WHITE);
    }

    /// Returns true if this contains blue.
    default boolean isBlue() {
        return contains(Color.BLUE);
    }

    /// Returns true if this contains black.
    default boolean isBlack() {
        return contains(Color.BLACK);
    }

    /// Returns true if this contains red.
    default boolean isRed() {
        return contains(Color.RED);
    }

    /// Returns true if this contains green.
    default boolean isGreen() {
        return contains(Color.GREEN);
    }

    /// Returns true if this contains at least one color.
    default boolean isColored() {
        return !isEmpty();
    }

    /// Returns true if this contains no colors.
    default boolean isColorless() {
        return isEmpty();
    }

    /// Returns true if this contains exactly one color.
    default boolean isMonoColored() {
        return count() == 1;
    }

    /// Returns true if this contains two or more colors.
    default boolean isMultiColored() {
        return count() >= 2;
    }

    /// Returns true if this contains all five colors.
    default boolean isAllColors() {
        return count() == 5;
    }

    /// Builder for Colors.
    interface Builder extends Characteristics.Builder<Color, Colors, Builder> {}
}
