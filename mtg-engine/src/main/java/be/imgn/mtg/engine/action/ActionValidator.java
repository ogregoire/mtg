package be.imgn.mtg.engine.action;

import be.imgn.mtg.engine.state.GameState;

/// Validates whether a player action is legal in the current game state.
///
/// Validation checks include:
/// - Does the player have priority?
/// - Is the timing correct (e.g., sorcery speed)?
/// - Can the player pay costs?
/// - Are restriction effects violated?
///
/// @see ValidationResult
/// @see ActionExecutor
public interface ActionValidator {

    /// Validates whether the given action can be legally taken.
    ///
    /// @param action the action to validate
    /// @param state the current game state
    /// @return the validation result (legal or illegal with reason)
    ValidationResult validate(PlayerAction action, GameState state);
}
