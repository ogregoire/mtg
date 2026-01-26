package be.imgn.mtg.engine.turn2;

import java.util.List;

/// A phase within a turn ({@mtg.rule 500.1}).
///
/// A turn consists of phases in a fixed order: beginning, first main, combat,
/// second main, and ending. The main phase occurs twice per turn.
public enum Phase {
    /// The beginning phase ({@mtg.rule 501}), containing untap, upkeep, and draw steps.
    BEGINNING(List.of(Step.UNTAP, Step.UPKEEP, Step.DRAW)),

    /// The main phase ({@mtg.rule 505}). Occurs twice per turn (pre-combat and post-combat).
    MAIN(List.of()),

    /// The combat phase ({@mtg.rule 506}), containing combat steps.
    COMBAT(List.of(
            Step.BEGINNING_OF_COMBAT,
            Step.DECLARE_ATTACKERS,
            Step.DECLARE_BLOCKERS,
            Step.COMBAT_DAMAGE,
            Step.END_OF_COMBAT)),

    /// The ending phase ({@mtg.rule 512}), containing end and cleanup steps.
    ENDING(List.of(Step.END, Step.CLEANUP));

    private final List<Step> steps;

    Phase(List<Step> steps) {
        this.steps = List.copyOf(steps);
    }

    /// Returns the steps that make up this phase.
    ///
    /// @return an unmodifiable list of steps in order
    public List<Step> steps() {
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
