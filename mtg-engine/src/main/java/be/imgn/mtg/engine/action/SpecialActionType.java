package be.imgn.mtg.engine.action;

/// Types of special actions as defined by Rule 116.
///
/// Special actions don't use the stack. Some require priority, others don't.
///
/// @see <a href="https://mtg.fandom.com/wiki/Special_action">Special Actions</a>
public enum SpecialActionType {

    /// Playing a land ({@mtg.rule 116.2a}).
    ///
    /// Requires priority. Limited to once per turn (by default).
    PLAY_LAND(true),

    /// Turning a face-down creature face up ({@mtg.rule 116.2b}).
    ///
    /// Does NOT require priority. Can be done any time the player
    /// could take an action.
    TURN_FACE_UP(false),

    /// Suspending a card from hand ({@mtg.rule 116.2c}).
    ///
    /// Requires priority. Can only be done during a main phase
    /// when the stack is empty.
    SUSPEND(true),

    /// Putting a companion into hand ({@mtg.rule 116.2d}).
    ///
    /// Requires priority. Can only be done once per game during
    /// a main phase when the stack is empty.
    COMPANION(true),

    /// Foretelling a card from hand.
    ///
    /// Requires priority. Can only be done during your turn.
    FORETELL(true);

    private final boolean requiresPriority;

    SpecialActionType(boolean requiresPriority) {
        this.requiresPriority = requiresPriority;
    }

    /// Returns whether this special action requires the player to have priority.
    ///
    /// @return true if priority is required, false otherwise
    public boolean requiresPriority() {
        return requiresPriority;
    }
}
