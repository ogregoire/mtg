package be.imgn.mtg.engine.oracle;

import be.imgn.mtg.engine.turn.Step;

/// Duration of a continuous effect (Rule 611.2).
public sealed interface Duration {

    /// Fixed, parameterless durations. Each value is a self-contained
    /// temporal scope that needs no further data.
    enum Fixed implements Duration {
        UNTIL_END_OF_TURN,
        UNTIL_YOUR_NEXT_TURN,
        UNTIL_END_OF_COMBAT,
        THIS_TURN,
        /// "During your turn, …" — scoped to turns the controller owns.
        DURING_YOUR_TURN,
        /// "On each of your turns" — recurring scope triggered every one
        /// of the controller's turns (e.g., Exploration: "You may play
        /// an additional land on each of your turns."). Distinct from
        /// [#DURING_YOUR_TURN], which scopes a continuous effect to the
        /// currently-active turn.
        EACH_YOUR_TURN,
        /// "During turns other than yours" — scoped to turns belonging
        /// to a player other than the controller (e.g., Mesa Lynx:
        /// "During turns other than yours, this creature gets +0/+2.").
        DURING_OTHERS_TURN
    }

    record UntilEvent(String description) implements Duration {}

    /// "until \[owner\]'s next \[step\]" — scoped to the next occurrence
    /// of a specific step owned by a specific player (Orcish Farmer:
    /// "until its controller's next untap step").
    record UntilNextStep(Subject owner, Step step) implements Duration {}

    record ForAsLongAs(String condition) implements Duration {}

    /// Creates an [UntilEvent] duration.
    static Duration untilEvent(String description) {
        return new UntilEvent(description);
    }

    /// Creates a [ForAsLongAs] duration.
    static Duration forAsLongAs(String condition) {
        return new ForAsLongAs(condition);
    }
}
