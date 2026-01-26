package be.imgn.mtg.engine.turn2;

import org.jspecify.annotations.Nullable;

import be.imgn.mtg.engine.game.Player;

/// Tracks turn progression, phases, steps, priority, and the game loop ({@mtg.rule 500}).
///
/// This is the main orchestrator for the turn system. It manages:
/// - Turn order and extra/skipped turns
/// - Phase and step progression
/// - Priority passing and stack resolution
/// - State-based action checks (internally)
///
/// Advancement through the game is driven by [#passPriority].
/// When all players pass with an empty stack, the tracker automatically advances
/// to the next step/phase/turn.
public interface TurnTracker2 {

    /// Starts the game with the specified starting player.
    ///
    /// Must be called before any other methods. The starting player's first
    /// draw step is automatically skipped per the rules.
    ///
    /// @param startingPlayer the player who takes the first turn
    /// @throws IllegalStateException if the game has already been started
    void startGame(Player startingPlayer);

    /// Returns the player whose turn it currently is.
    ///
    /// @return the active player
    Player activePlayer();

    /// Returns the current turn.
    ///
    /// @return the current turn containing turn number and active player
    Turn currentTurn();

    /// Returns the current phase.
    ///
    /// @return the current phase, or null if between turns
    @Nullable
    Phase currentPhase();

    /// Returns the current step.
    ///
    /// @return the current step, or null if in main phase or between phases
    @Nullable
    Step currentStep();

    /// Returns the number of times the current phase has occurred this turn.
    ///
    /// For main phases, both occurrences count (pre-combat = 1, post-combat = 2).
    ///
    /// @return 1 for first occurrence, 2 for second occurrence, etc.; 0 if no phase active
    int currentPhaseOccurrence();

    /// Returns the number of times the current step has occurred within the current phase.
    ///
    /// Resets when a new phase begins.
    ///
    /// @return 1 for first occurrence, 2 for second, etc.; 0 if no step active
    int currentStepOccurrenceInPhase();

    /// Adds an extra turn for the specified player after the current turn ({@mtg.rule 500.7}).
    ///
    /// Extra turns are stored in LIFO order.
    ///
    /// @param player the player who gets the extra turn
    void addExtraTurn(Player player);

    /// Causes the specified player to skip their next turn ({@mtg.rule 500.11}).
    ///
    /// @param player the player whose next turn to skip
    void skipNextTurn(Player player);

    /// Skips all future occurrences of the specified step for the specified player.
    ///
    /// Example: "You skip your draw step."
    ///
    /// @param player the player affected
    /// @param step the step to skip permanently
    void skipAllSteps(Player player, Step step);

    /// Skips the next occurrence of the specified step for the specified player.
    ///
    /// Example: "Target player skips their next draw step."
    ///
    /// @param player the player affected
    /// @param step the step to skip once
    void skipNextOccurrence(Player player, Step step);

    /// Skips all steps in the specified phase for the specified player's next turn.
    ///
    /// Example: "Target opponent skips all combat phases of their next turn."
    ///
    /// @param player the player affected
    /// @param phase the phase to skip
    void skipPhaseNextTurn(Player player, Phase phase);

    /// Skips the next occurrence of the specified phase in the current turn.
    ///
    /// Only affects the first occurrence of that phase that hasn't been reached yet.
    /// Example: "Skip your next combat phase this turn."
    ///
    /// @param phase the phase to skip
    void skipPhaseThisTurn(Phase phase);

    /// Inserts an additional phase after the current step.
    ///
    /// The phase will have all its normal steps.
    /// Example: "After this main phase, there is an additional combat phase."
    ///
    /// @param phase the phase to insert
    void insertPhaseAfterCurrent(Phase phase);

    /// Inserts a single step after the current step.
    ///
    /// Useful for repeating steps like cleanup when SBAs or triggers occur.
    ///
    /// @param step the step to insert
    void insertStepAfterCurrent(Step step);

    /// Ends the current turn immediately ({@mtg.rule 720.4}).
    ///
    /// Used by effects like Time Stop and Sundial of the Infinite.
    /// Exiles all spells and abilities on the stack, then proceeds to cleanup.
    void endTurnEarly();

    /// Checks if the specified player currently has priority.
    ///
    /// @param player the player to check
    /// @return true if the player has priority
    boolean hasPriority(Player player);

    /// The specified player passes priority ({@mtg.rule 117.3d}).
    ///
    /// When all players pass in succession:
    /// - If the stack is not empty, the top object resolves
    /// - If the stack is empty, the current step/phase ends
    ///
    /// @param player the player passing priority
    void passPriority(Player player);
}
