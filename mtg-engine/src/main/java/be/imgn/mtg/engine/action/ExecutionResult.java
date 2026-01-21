package be.imgn.mtg.engine.action;

import java.util.List;

import be.imgn.mtg.engine.event.GameEvent;

/// Result of executing a player action.
///
/// Execution results indicate whether the action completed successfully,
/// was aborted by the player, or became illegal during execution.
public sealed interface ExecutionResult {

    /// The action completed successfully.
    ///
    /// @param events the game events produced by the action (may be empty)
    record Success(List<GameEvent> events) implements ExecutionResult {}

    /// The action was aborted by the player.
    ///
    /// This can happen when a player backs out during targeting, mode selection,
    /// or other choices made during action execution.
    ///
    /// @param reason a description of why the action was aborted
    record Aborted(String reason) implements ExecutionResult {}

    /// The action became illegal during execution.
    ///
    /// This can happen if game state changes between validation and execution
    /// (rare but possible due to triggers or replacement effects).
    ///
    /// @param reason a description of why the action is now illegal
    record Illegal(String reason) implements ExecutionResult {}
}
