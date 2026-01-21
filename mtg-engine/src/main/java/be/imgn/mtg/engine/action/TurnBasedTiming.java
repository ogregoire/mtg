package be.imgn.mtg.engine.action;

/// Timing points at which turn-based actions occur ({@mtg.rule 703}).
///
/// Turn-based actions happen automatically at specific points during a turn.
/// They don't use the stack and don't involve player choice.
public enum TurnBasedTiming {

    /// Phasing occurs at the beginning of the untap step ({@mtg.rule 502.1}).
    ///
    /// Permanents with phasing phase out, and phased-out permanents phase in.
    UNTAP_STEP_PHASING,

    /// Day/night check at the beginning of the untap step ({@mtg.rule 502.2}).
    ///
    /// If it became day or night during the previous turn, any day/night
    /// triggered abilities trigger.
    UNTAP_STEP_DAY_NIGHT,

    /// Active player untaps their permanents ({@mtg.rule 502.3}).
    ///
    /// The active player untaps all permanents they control that untap
    /// during the untap step.
    UNTAP_STEP_UNTAP,

    /// Active player draws a card at the beginning of the draw step ({@mtg.rule 504.1}).
    ///
    /// This is the normal once-per-turn draw. The starting player skips this
    /// draw on the first turn of the game.
    DRAW_STEP_DRAW,

    /// Active player discards down to maximum hand size ({@mtg.rule 514.1}).
    ///
    /// If the active player has more cards in hand than their maximum hand size,
    /// they discard cards until they have that many.
    CLEANUP_DISCARD,

    /// Damage is removed from permanents and "until end of turn" effects end ({@mtg.rule 514.2}).
    ///
    /// All damage marked on permanents is removed, and effects that last
    /// "until end of turn" or "this turn" expire.
    CLEANUP_REMOVE_DAMAGE
}
