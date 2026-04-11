package be.imgn.mtg.engine.oracle;

/// Duration of a continuous effect (Rule 611.2).
public sealed interface Duration {
    record UntilEndOfTurn() implements Duration {}

    record UntilYourNextTurn() implements Duration {}

    record UntilEndOfCombat() implements Duration {}

    record ThisTurn() implements Duration {}

    record UntilEvent(String description) implements Duration {}

    record ForAsLongAs(String condition) implements Duration {}

    /// Creates an {@link UntilEndOfTurn} duration.
    static Duration untilEndOfTurn() {
        return new UntilEndOfTurn();
    }

    /// Creates an {@link UntilYourNextTurn} duration.
    static Duration untilYourNextTurn() {
        return new UntilYourNextTurn();
    }

    /// Creates an {@link UntilEndOfCombat} duration.
    static Duration untilEndOfCombat() {
        return new UntilEndOfCombat();
    }

    /// Creates a {@link ThisTurn} duration.
    static Duration thisTurn() {
        return new ThisTurn();
    }

    /// Creates an {@link UntilEvent} duration.
    static Duration untilEvent(String description) {
        return new UntilEvent(description);
    }

    /// Creates a {@link ForAsLongAs} duration.
    static Duration forAsLongAs(String condition) {
        return new ForAsLongAs(condition);
    }
}
