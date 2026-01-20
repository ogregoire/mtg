package be.imgn.mtg.engine.turn.internal.steps;

import org.jspecify.annotations.Nullable;

import be.imgn.mtg.engine.state.GameState;
import be.imgn.mtg.engine.turn.Step;
import be.imgn.mtg.engine.turn.StepType;

/// Pseudo-step representing the main phase ({@mtg.rule 505}).
///
/// The main phase doesn't have steps in the traditional sense, but this class
/// provides a Step-like interface for turn-based actions that occur in the
/// main phase, such as placing saga counters ({@mtg.rule 714.3}).
///
/// The occurrence field distinguishes between the first main phase (precombat, 1)
/// and the second main phase (postcombat, 2).
public final class MainPhaseStep implements Step {

    private final int occurrence;

    /// Creates a main phase step.
    ///
    /// @param occurrence 1 for pre-combat main phase, 2 for post-combat main phase
    public MainPhaseStep(int occurrence) {
        this.occurrence = occurrence;
    }

    /// Returns null since the main phase is not a step.
    ///
    /// @return null
    @Override
    public @Nullable StepType type() {
        return null;
    }

    @Override
    public int occurrence() {
        return occurrence;
    }

    @Override
    public void performTurnBasedActions(GameState gameState) {
        // TODO: Saga chapter (Rule 714.3)
        // The active player puts a lore counter on each Saga they control
        // as a turn-based action immediately after their main phase begins

        // TODO: Check attraction roll (Rule 702.160a)
        // At the beginning of the first main phase, the active player
        // may roll to visit their attractions
    }

    @Override
    public boolean hasPriority() {
        return true;
    }

    @Override
    public void performEndActions(GameState gameState) {
        // No special end actions for main phase
    }
}
