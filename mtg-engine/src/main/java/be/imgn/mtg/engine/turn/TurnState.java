package be.imgn.mtg.engine.turn;

import java.util.Optional;

import be.imgn.mtg.engine.game.Player;

/// Tracks the current turn state including turn order, active player, occurrences, and skips ({@mtg.rule 500}).
///
/// The turn state maintains:
/// - The current turn number
/// - The current active player
/// - The turn order (which player goes next)
/// - Extra turns queue (LIFO - last in, first out)
/// - Phase and step occurrence counts
/// - Skip tracking for phases, steps, and turns ({@mtg.rule 500.11})
///
/// @see TurnTracker
public interface TurnState {

    /// Returns the current turn number (1-indexed).
    ///
    /// @return the current turn number
    int turnNumber();

    /// Returns the current active player ({@mtg.rule 102.1}).
    ///
    /// The active player is the player whose turn it is.
    ///
    /// @return the active player
    Player activePlayer();

    /// Advances to the next turn and returns the new active player.
    ///
    /// If there are extra turns queued, the next extra turn is taken.
    /// Otherwise, the next player in turn order becomes active.
    ///
    /// @return the player whose turn it now is
    Player nextTurn();

    /// Removes a player from the turn order ({@mtg.rule 800.4}).
    ///
    /// When a player leaves the game, they are removed from the turn order.
    /// If they had any pending extra turns, those are also removed.
    ///
    /// @param player the player to remove
    void removePlayer(Player player);

    /// Queues an extra turn for a player ({@mtg.rule 500.7}).
    ///
    /// Extra turns are stored in a LIFO queue. When multiple players
    /// get extra turns simultaneously, APNAP order determines the order
    /// they're added to the queue.
    ///
    /// @param player the player who gets the extra turn
    void addExtraTurn(Player player);

    /// Returns the player who will take the next extra turn, if any.
    ///
    /// @return the player for the next extra turn, or empty if no extra turns queued
    Optional<Player> peekExtraTurn();

    // ===== Occurrence tracking (merged from OccurrenceTracker) =====

    /// Increments the occurrence count for a phase and returns the new count.
    ///
    /// @param phase the phase type
    /// @return the occurrence number (1 for first occurrence, 2 for second, etc.)
    int incrementOccurrence(PhaseType phase);

    /// Increments the occurrence count for a step and returns the new count.
    ///
    /// @param step the step type
    /// @return the occurrence number (1 for first occurrence, 2 for second, etc.)
    int incrementOccurrence(StepType step);

    /// Returns the current occurrence count for a phase.
    ///
    /// @param phase the phase type
    /// @return the current count (0 if the phase hasn't occurred yet this turn)
    int occurrence(PhaseType phase);

    /// Returns the current occurrence count for a step.
    ///
    /// @param step the step type
    /// @return the current count (0 if the step hasn't occurred yet this turn)
    int occurrence(StepType step);

    /// Resets all occurrence counts to zero.
    ///
    /// This is called at the start of each turn.
    void resetOccurrences();

    // ===== Skip tracking (merged from SkipTracker) =====

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
    /// any per-turn skip effects. Note: turn skips persist until consumed.
    void resetSkipsForNewTurn();
}
