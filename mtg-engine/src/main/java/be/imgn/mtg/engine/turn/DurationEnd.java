package be.imgn.mtg.engine.turn;

/// Enumeration of duration endpoints for continuous effects ({@mtg.rule 611.2}).
///
/// Effects can last for various durations. This enum represents the common
/// duration endpoints used by cards and abilities.
///
/// @see DurationTracker
public enum DurationEnd {
    /// Until the cleanup step of this turn ("until end of turn", {@mtg.rule 514.2}).
    UNTIL_END_OF_TURN,

    /// For the entire turn ("this turn"). Unlike "until end of turn", effects with
    /// "this turn" duration end at the very end of the turn, not during cleanup.
    THIS_TURN,

    /// Until the end of combat step of this combat phase ({@mtg.rule 511}).
    UNTIL_END_OF_COMBAT,

    /// Until the beginning of your next turn. The effect ends immediately before
    /// the untap step of your next turn.
    UNTIL_YOUR_NEXT_TURN,

    /// Until the end of your next turn. The effect ends during the cleanup step
    /// of your next turn.
    UNTIL_END_OF_YOUR_NEXT_TURN,

    /// Until the beginning of the next end step. This is used by some effects
    /// that want to return something at the beginning of an end step.
    UNTIL_NEXT_END_STEP,

    /// Until your next upkeep. The effect ends at the beginning of your next
    /// upkeep step.
    UNTIL_YOUR_NEXT_UPKEEP
}
