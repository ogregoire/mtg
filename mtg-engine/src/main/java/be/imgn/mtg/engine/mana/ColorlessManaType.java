package be.imgn.mtg.engine.mana;

import java.util.Optional;

import be.imgn.mtg.engine.characteristics.Color;

/// Colorless mana type.
///
/// Colorless is not a color ({@mtg.rule 105.4}).
public enum ColorlessManaType implements ManaType {
    /// Colorless mana, produced by various sources.
    COLORLESS;

    @Override
    public Optional<Color> color() {
        return Optional.empty();
    }

    @Override
    public boolean isColored() {
        return false;
    }
}
