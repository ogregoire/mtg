package be.imgn.mtg.engine.action;

import be.imgn.mtg.engine.state.GameState;

/// Executes validated player actions.
///
/// The executor dispatches actions to appropriate handlers:
/// - Pass → simply record the pass in PrioritySystem
/// - PlayLand → delegate to SpecialActionHandler
/// - SpecialAction → delegate to SpecialActionHandler
/// - CastSpell → delegate to SpellCastingProcess (future work)
/// - ActivateAbility → delegate to AbilityActivationProcess (future work)
///
/// After any action that is not a Pass, the executor resets the priority
/// system to clear pass state.
///
/// @see ActionValidator
/// @see ExecutionResult
public interface ActionExecutor {

    /// Executes a player action.
    ///
    /// The action should have been validated beforehand, but execution
    /// may still fail if game state changed between validation and execution.
    ///
    /// @param action the action to execute
    /// @param state the current game state
    /// @return the execution result
    ExecutionResult execute(PlayerAction action, GameState state);
}
