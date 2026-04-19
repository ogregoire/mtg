package be.imgn.mtg.engine.oracle;

/// Duration of a continuous effect (Rule 611.2).
public sealed interface Duration {
    enum UntilEndOfTurn implements Duration {
        UNTIL_END_OF_TURN
    }

    enum UntilYourNextTurn implements Duration {
        UNTIL_YOUR_NEXT_TURN
    }

    enum UntilEndOfCombat implements Duration {
        UNTIL_END_OF_COMBAT
    }

    enum ThisTurn implements Duration {
        THIS_TURN
    }

    /// "During your turn, …" — scoped to turns the controller owns.
    enum DuringYourTurn implements Duration {
        DURING_YOUR_TURN
    }

    record UntilEvent(String description) implements Duration {}

    record ForAsLongAs(String condition) implements Duration {}

    /// Returns the {@link UntilEndOfTurn} singleton.
    static Duration untilEndOfTurn() {
        return UntilEndOfTurn.UNTIL_END_OF_TURN;
    }

    /// Returns the {@link UntilYourNextTurn} singleton.
    static Duration untilYourNextTurn() {
        return UntilYourNextTurn.UNTIL_YOUR_NEXT_TURN;
    }

    /// Returns the {@link UntilEndOfCombat} singleton.
    static Duration untilEndOfCombat() {
        return UntilEndOfCombat.UNTIL_END_OF_COMBAT;
    }

    /// Returns the {@link ThisTurn} singleton.
    static Duration thisTurn() {
        return ThisTurn.THIS_TURN;
    }

    /// Returns the {@link DuringYourTurn} singleton.
    static Duration duringYourTurn() {
        return DuringYourTurn.DURING_YOUR_TURN;
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
