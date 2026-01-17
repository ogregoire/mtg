package be.imgn.mtg.engine.characteristics;

/// Counter types in Magic ({@mtg.rule 122}).
///
/// A counter is a marker placed on an object or player that modifies its characteristics or
/// interacts with a rule, ability, or effect. Counters with the same name are interchangeable.
public enum CounterType {
    /// Loyalty counters, used on planeswalkers.
    LOYALTY("loyalty"),
    /// +1/+1 counters, which increase power and toughness.
    PLUS_ONE_PLUS_ONE("+1/+1"),
    /// -1/-1 counters, which decrease power and toughness.
    MINUS_ONE_MINUS_ONE("-1/-1");

    private final String text;

    CounterType(String text) {
        this.text = text;
    }

    /// Returns the text representation of this counter type.
    ///
    /// @return the counter type text
    public String text() {
        return text;
    }
}
