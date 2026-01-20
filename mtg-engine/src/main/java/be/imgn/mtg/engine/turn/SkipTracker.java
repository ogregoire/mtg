package be.imgn.mtg.engine.turn;

import be.imgn.mtg.engine.game.Player;

/// Tracks skipped phases, steps, and turns ({@mtg.rule 500.11}).
///
/// Effects can cause phases, steps, or turns to be skipped. When something is skipped:
/// - Turn-based actions don't happen
/// - No priority is given
/// - "At beginning of X" triggers don't trigger
/// - "Until end of X" effects still end ({@mtg.rule 614.10})
///
/// @see TurnTracker
public interface SkipTracker {

    /// Checks if a phase should be skipped for the current turn.
    ///
    /// @param phaseType the type of phase to check
    /// @return true if the phase should be skipped
    boolean isSkipped(PhaseType phaseType);

    /// Checks if a step should be skipped for the current turn.
    ///
    /// @param stepType the type of step to check
    /// @return true if the step should be skipped
    boolean isSkipped(StepType stepType);

    /// Checks if a player's next turn should be skipped.
    ///
    /// @param player the player to check
    /// @return true if the player's next turn should be skipped
    boolean shouldSkipNextTurn(Player player);

    /// Marks a phase to be skipped for the current turn.
    ///
    /// @param phaseType the type of phase to skip
    void skipPhase(PhaseType phaseType);

    /// Marks a step to be skipped for the current turn.
    ///
    /// @param stepType the type of step to skip
    void skipStep(StepType stepType);

    /// Marks a player's next turn to be skipped.
    ///
    /// @param player the player whose turn to skip
    void skipNextTurn(Player player);

    /// Clears skip state for a player's turn (called when the skip is consumed).
    ///
    /// @param player the player whose skip state to clear
    void clearTurnSkip(Player player);

    /// Resets all phase and step skips for the start of a new turn.
    ///
    /// This should be called at the beginning of each turn to clear
    /// any per-turn skip effects.
    void resetForNewTurn();
}
