package be.imgn.mtg.engine.characteristics;

/// Counter types in Magic ({@mtg.rule 122}).
///
/// A counter is a marker placed on an object or player that modifies its characteristics or
/// interacts with a rule, ability, or effect. Counters with the same name are interchangeable.
public enum CounterType {
    LOYALTY("loyalty"),
    PLUS_ONE_PLUS_ONE("+1/+1"),
    MINUS_ONE_MINUS_ONE("-1/-1");

    private final String text;

    CounterType(String text) {
        this.text = text;
    }

    public String text() {
        return text;
    }
}
