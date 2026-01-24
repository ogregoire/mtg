package be.imgn.mtg.engine.ability.internal.parser.selector;

/// Comparison operators for numeric values in oracle text.
public enum Comparison {
    /// Equal to.
    EQUAL,
    /// Less than.
    LESS,
    /// Less than or equal to.
    LESS_OR_EQUAL,
    /// Greater than.
    GREATER,
    /// Greater than or equal to.
    GREATER_OR_EQUAL;

    /// Tests whether the actual value satisfies this comparison against the expected value.
    public boolean test(int actual, int expected) {
        return switch (this) {
            case EQUAL -> actual == expected;
            case LESS -> actual < expected;
            case LESS_OR_EQUAL -> actual <= expected;
            case GREATER -> actual > expected;
            case GREATER_OR_EQUAL -> actual >= expected;
        };
    }
}
