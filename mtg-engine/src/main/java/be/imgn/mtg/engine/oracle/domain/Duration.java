package be.imgn.mtg.engine.oracle.domain;

import be.imgn.mtg.engine.turn.Step;

/// Duration of a continuous effect (Rule 611.2).
public sealed interface Duration {

    /// Fixed, parameterless durations. Each value is a self-contained
    /// temporal scope that needs no further data.
    enum Fixed implements Duration {
        UNTIL_END_OF_TURN,
        UNTIL_YOUR_NEXT_TURN,
        /// "Until the end of your next turn" — scoped to the end of the
        /// player's next turn (distinct from [#UNTIL_YOUR_NEXT_TURN] which
        /// expires at the start of that turn).
        UNTIL_END_OF_YOUR_NEXT_TURN,
        UNTIL_END_OF_COMBAT,
        THIS_TURN,
        /// "This combat" — scoped to the current combat phase (Yuan
        /// Shao's Infantry: "… can't be blocked this combat.").
        THIS_COMBAT,
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
        DURING_OTHERS_TURN,
        /// "During combat" — scoped to any combat phase (Basandra,
        /// Battle Seraph: "Players can't cast spells during combat.").
        DURING_COMBAT
    }

    record UntilEvent(String description) implements Duration {}

    /// "until \[owner\]'s next \[step\]" — scoped to the next occurrence
    /// of a specific step owned by a specific player (Orcish Farmer:
    /// "until its controller's next untap step").
    record UntilNextStep(Subject owner, Step step) implements Duration {}

    /// "during \[owner\]'s next turn" — scoped to the specified
    /// player's next turn (Sphinx's Decree: "Each opponent can't cast
    /// instant or sorcery spells during that player's next turn.").
    record DuringNextTurn(Subject.PlayerRef owner) implements Duration {}

    /// "during \[owner\]'s \[step\]" — recurring scope tied to a
    /// specific step in the owner's turn (Final-Word Phantom: "During
    /// each opponent's end step, you may cast spells as though they
    /// had flash."). Distinct from [#UntilNextStep] (one-shot, next
    /// occurrence) and [#DuringNextTurn] (full turn).
    record DuringStep(Subject.PlayerRef owner, Step step) implements Duration {}

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
