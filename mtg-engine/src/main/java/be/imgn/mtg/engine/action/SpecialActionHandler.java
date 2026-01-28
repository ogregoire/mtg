package be.imgn.mtg.engine.action;

import be.imgn.mtg.engine.state.GameState;

/// Handles special actions that don't use the stack ({@mtg.rule 116}).
///
/// Special actions include playing lands, turning face-down creatures face up,
/// suspending cards, and other actions defined in Rule 116.
///
/// @see SpecialActionType
public interface SpecialActionHandler {

    /// Plays a land from the player's hand.
    ///
    /// Playing a land is a special action that:
    /// - Doesn't use the stack
    /// - Uses up one of the player's land drops
    /// - Can only be done during the player's main phase with the stack empty
    ///
    /// @param action the play land action
    /// @param state the current game state
    /// @return the execution result
    ExecutionResult playLand(PlayerAction.PlayLand action, GameState state);

    /// Executes a special action.
    ///
    /// @param action the special action to execute
    /// @param state the current game state
    /// @return the execution result
    ExecutionResult execute(PlayerAction.SpecialAction action, GameState state);
}
