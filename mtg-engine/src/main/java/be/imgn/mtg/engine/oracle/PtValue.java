package be.imgn.mtg.engine.oracle;

import org.jspecify.annotations.Nullable;

/// A power/toughness value pair (e.g., 2/3 or X/X). Either side may
/// be null to represent an asymmetric assignment ("base power N" with
/// toughness unchanged, "base toughness N" with power unchanged). The
/// word *base* that signals rule 613.4 / layer 7b is carried by the
/// surrounding [Effect.SetBasePT] — PtValue is just the
/// numeric payload.
public record PtValue(@Nullable Amount power, @Nullable Amount toughness) {
    /// Convenience constructor for fixed integer P/T values.
    public PtValue(int power, int toughness) {
        this(Amount.exact(power), Amount.exact(toughness));
    }
}
