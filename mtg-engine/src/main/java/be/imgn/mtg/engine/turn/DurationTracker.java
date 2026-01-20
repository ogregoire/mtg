package be.imgn.mtg.engine.turn;

/// Tracks effects with durations and handles their expiration ({@mtg.rule 611.2}).
///
/// Effects can have various durations like "until end of turn," "until end of combat,"
/// or "until your next turn." The duration tracker manages when these effects expire.
///
/// @see DurationEnd
public interface DurationTracker {

    /// Expires effects that end at the beginning of a step.
    ///
    /// Called before turn-based actions in a step. Used for "until upkeep" effects.
    ///
    /// @param step the step that is beginning
    void expireUntilStep(Step step);

    /// Expires effects that end at the end of a step.
    ///
    /// Called at the end of a step, after priority passes.
    ///
    /// @param step the step that is ending
    void expireUntilEndOfStep(Step step);

    /// Expires effects that end "until end of turn" ({@mtg.rule 514.2}).
    ///
    /// This is called during the cleanup step. Effects with "until end of turn"
    /// duration expire at this time.
    void expireUntilEndOfTurn();

    /// Expires effects that end "until end of combat" ({@mtg.rule 511}).
    ///
    /// This is called at the end of the combat phase.
    void expireUntilEndOfCombat();

    /// Expires effects that end at the start of a player's next turn.
    ///
    /// Called at the very beginning of a turn, before the untap step,
    /// for effects that last "until your next turn."
    ///
    /// @param turnState the current turn state
    void expireUntilNextTurn(TurnState turnState);
}
