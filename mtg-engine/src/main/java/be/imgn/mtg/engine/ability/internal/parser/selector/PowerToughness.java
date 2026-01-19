package be.imgn.mtg.engine.ability.internal.parser.selector;

/// Represents a creature's power and toughness.
///
/// Power and toughness can be exact values (like 1/1) or variable (like X/X).
///
/// @param power the creature's power
/// @param toughness the creature's toughness
public record PowerToughness(Amount power, Amount toughness) {

    /// Creates a PowerToughness with exact integer values.
    public static PowerToughness of(int power, int toughness) {
        return new PowerToughness(new Amount.Exact(power), new Amount.Exact(toughness));
    }

    /// Returns true if both power and toughness are exact (non-variable) values.
    public boolean isExact() {
        return power instanceof Amount.Exact && toughness instanceof Amount.Exact;
    }

    /// Returns the power as an int, or throws if power is not exact.
    public int powerValue() {
        if (power instanceof Amount.Exact exact) {
            return exact.value();
        }
        throw new IllegalStateException("Power is not an exact value: " + power);
    }

    /// Returns the toughness as an int, or throws if toughness is not exact.
    public int toughnessValue() {
        if (toughness instanceof Amount.Exact exact) {
            return exact.value();
        }
        throw new IllegalStateException("Toughness is not an exact value: " + toughness);
    }
}
