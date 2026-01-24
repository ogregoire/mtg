package be.imgn.mtg.engine.turn.internal;

import org.jspecify.annotations.Nullable;

import be.imgn.mtg.engine.state.GameState;
import be.imgn.mtg.engine.turn.StepType;

/// A step within a phase of a turn ({@mtg.rule 500.2}).
///
/// Steps are the smallest units of the turn structure. Each step has a type,
/// an occurrence number (for steps that can happen multiple times in a turn),
/// and specific behaviors for turn-based actions and priority passing.
///
/// @see StepType
/// @see Phase
public interface Step {

    /// Returns the type of this step, or null for pseudo-steps like main phase.
    ///
    /// @return the step type, or null if this is a pseudo-step representing a phase
    @Nullable
    StepType type();

    /// Returns the occurrence number of this step in the current turn.
    ///
    /// Most steps occur only once per turn (occurrence = 1), but some steps
    /// like combat damage can occur multiple times (first strike, double strike).
    ///
    /// @return the 1-indexed occurrence number
    int occurrence();

    /// Performs the turn-based actions for this step ({@mtg.rule 703.4}).
    ///
    /// Turn-based actions are automatic game actions that happen at specific times.
    /// For example:
    /// - Untap step: Phasing, untapping ({@mtg.rule 502.1}, {@mtg.rule 502.3})
    /// - Draw step: Active player draws ({@mtg.rule 504.1})
    /// - Cleanup step: Discard to hand size, remove damage ({@mtg.rule 514.1}, {@mtg.rule 514.2})
    ///
    /// @param gameState the current game state
    void performTurnBasedActions(GameState gameState);

    /// Returns whether players receive priority during this step.
    ///
    /// Most steps give players priority, but the untap step and cleanup step
    /// normally do not ({@mtg.rule 502.4}, {@mtg.rule 514.3}).
    ///
    /// @return true if players receive priority during this step
    boolean hasPriority();

    /// Performs any actions that occur at the end of this step.
    ///
    /// This is called after the priority round (if any) completes but before
    /// the step ends. Used for effects that expire "at end of step."
    ///
    /// @param gameState the current game state
    void performEndActions(GameState gameState);
}
