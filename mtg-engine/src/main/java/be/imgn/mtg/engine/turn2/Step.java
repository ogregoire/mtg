package be.imgn.mtg.engine.turn2;

/// A step within a phase of a turn ({@mtg.rule 500.2}).
///
/// Steps are the smallest units of the turn structure.
public enum Step {
    // Beginning phase steps (Rule 501)
    /// The untap step ({@mtg.rule 502}). No player receives priority during this step.
    UNTAP(false),
    /// The upkeep step ({@mtg.rule 503}). Players receive priority.
    UPKEEP(true),
    /// The draw step ({@mtg.rule 504}). Active player draws, then players receive priority.
    DRAW(true),

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

    Step(boolean hasPriority) {
        this.hasPriority = hasPriority;
    }

    /// Returns whether players normally receive priority during this step.
    ///
    /// @return true if players normally receive priority during this step
    public boolean hasPriority() {
        return hasPriority;
    }
}
