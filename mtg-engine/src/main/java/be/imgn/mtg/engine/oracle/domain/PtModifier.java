package be.imgn.mtg.engine.oracle.domain;

/// A power/toughness modification — two independent [Component] values
/// for power and toughness. Each component is either a fixed signed integer
/// (`+2`, `-1`) or a signed `X` (`+X`, `-X`), allowing mixed modifiers like
/// `+X/+0` or `+2/-X`.
public record PtModifier(Component power, Component toughness, int multiplier) {

    /// Canonical 2-arg constructor — `multiplier` defaults to 1 (no
    /// scaling).
    public PtModifier(Component power, Component toughness) {
        this(power, toughness, 1);
    }

    /// One side (power or toughness) of a P/T modifier.
    public sealed interface Component {
        /// Signed integer: `+2`, `-1`, `+0`.
        record Fixed(int value) implements Component {}

        /// Signed `X`: `+X` has sign `+1`, `-X` has sign `-1`. The magnitude
        /// of `X` is resolved at effect resolution time.
        record Variable(int sign) implements Component {}
    }

    /// Factory for the common fully-fixed case (`+2/+1`, `-1/-1`).
    public static PtModifier fixed(int powerMod, int toughnessMod) {
        return new PtModifier(new Component.Fixed(powerMod), new Component.Fixed(toughnessMod));
    }
}
