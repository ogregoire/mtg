package be.imgn.mtg.engine.turn;

import java.util.List;

/// Enumeration of all phase types in a turn ({@mtg.rule 500.1}).
///
/// A turn consists of phases in a fixed order: beginning, first main, combat,
/// second main, and ending. The main phase uses a single type with occurrence
/// tracking to distinguish between first and second main phases.
///
/// @see StepType
public enum PhaseType {
    /// The beginning phase ({@mtg.rule 501}), containing untap, upkeep, and draw steps.
    BEGINNING(List.of(StepType.UNTAP, StepType.UPKEEP, StepType.DRAW)),

    /// The main phase ({@mtg.rule 505}). Occurs twice per turn (pre-combat and post-combat).
    /// Use occurrence tracking to distinguish between first (1) and second (2) main phase.
    MAIN(List.of()),

    /// The combat phase ({@mtg.rule 506}), containing beginning of combat, declare attackers,
    /// declare blockers, combat damage, and end of combat steps.
    COMBAT(List.of(
            StepType.BEGINNING_OF_COMBAT,
            StepType.DECLARE_ATTACKERS,
            StepType.DECLARE_BLOCKERS,
            StepType.COMBAT_DAMAGE,
            StepType.END_OF_COMBAT)),

    /// The ending phase ({@mtg.rule 512}), containing end and cleanup steps.
    ENDING(List.of(StepType.END, StepType.CLEANUP));

    private final List<StepType> steps;

    PhaseType(List<StepType> steps) {
        this.steps = List.copyOf(steps);
    }

    /// Returns the steps that make up this phase.
    ///
    /// @return an unmodifiable list of step types in order
    public List<StepType> steps() {
        return steps;
    }

    /// Returns whether this phase has steps.
    ///
    /// The main phase has no steps; it's a single phase where players
    /// can cast sorcery-speed spells.
    ///
    /// @return true if this phase has steps
    public boolean hasSteps() {
        return !steps.isEmpty();
    }
}
