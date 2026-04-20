package be.imgn.mtg.engine.oracle;

import org.jspecify.annotations.Nullable;

/// A power/toughness value pair (e.g., 2/3 or X/X). `toughness` may
/// be null for power-only phrases (Singing Tree: "has base power 0 until
/// end of turn").
public record PtValue(Amount power, @Nullable Amount toughness) {
    /// Convenience constructor for fixed integer P/T values.
    public PtValue(int power, int toughness) {
        this(Amount.exact(power), Amount.exact(toughness));
    }
}
