package be.imgn.mtg.engine.oracle;

/// A power/toughness value pair (e.g., 2/3 or X/X).
public record PtValue(Amount power, Amount toughness) {
    /// Convenience constructor for fixed integer P/T values.
    public PtValue(int power, int toughness) {
        this(Amount.exact(power), Amount.exact(toughness));
    }
}
