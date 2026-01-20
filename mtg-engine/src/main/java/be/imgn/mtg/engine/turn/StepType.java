package be.imgn.mtg.engine.turn;

/// Enumeration of all step types in a turn ({@mtg.rule 500.1}).
///
/// Steps are subdivisions of phases. Some steps have no special actions associated with them;
/// they exist mainly to allow triggered abilities and state-based actions to happen at
/// more discrete times.
///
/// @see PhaseType
public enum StepType {
    // Beginning phase steps (Rule 501)
    /// The untap step ({@mtg.rule 502}). No player receives priority during this step.
    UNTAP(false),
    /// The upkeep step ({@mtg.rule 503}). Players receive priority.
    UPKEEP(true),
    /// The draw step ({@mtg.rule 504}). Active player draws, then players receive priority.
    DRAW(true),

    // Main phase has no steps - it is a single phase

    // Combat phase steps (Rule 506)
    /// The beginning of combat step ({@mtg.rule 507}). Players receive priority.
    BEGINNING_OF_COMBAT(true),
    /// The declare attackers step ({@mtg.rule 508}). Players receive priority.
    DECLARE_ATTACKERS(true),
    /// The declare blockers step ({@mtg.rule 509}). Players receive priority.
    DECLARE_BLOCKERS(true),
    /// The combat damage step ({@mtg.rule 510}). Players receive priority.
    COMBAT_DAMAGE(true),
    /// The end of combat step ({@mtg.rule 511}). Players receive priority.
    END_OF_COMBAT(true),

    // Ending phase steps (Rule 512)
    /// The end step ({@mtg.rule 513}). Players receive priority.
    END(true),
    /// The cleanup step ({@mtg.rule 514}). Normally no player receives priority.
    CLEANUP(false);

    private final boolean hasPriority;

    StepType(boolean hasPriority) {
        this.hasPriority = hasPriority;
    }

    /// Returns whether players normally receive priority during this step.
    ///
    /// Note: The cleanup step normally has no priority pass, but if state-based actions
    /// are performed or triggered abilities are put on the stack during cleanup,
    /// players do receive priority ({@mtg.rule 514.3a}).
    ///
    /// @return true if players normally receive priority during this step
    public boolean hasPriority() {
        return hasPriority;
    }
}
