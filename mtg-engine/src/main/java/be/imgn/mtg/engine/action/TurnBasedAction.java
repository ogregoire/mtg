package be.imgn.mtg.engine.action;

import be.imgn.mtg.engine.event.GameEventProcessor;
import be.imgn.mtg.engine.state.GameState;

/// An action that happens automatically at a specific point in the turn ({@mtg.rule 703}).
///
/// Turn-based actions don't use the stack and don't involve player choice.
/// They are performed by the game engine at specific timing points.
///
/// @see TurnBasedTiming
/// @see TurnBasedActionRegistry
public interface TurnBasedAction {

    /// Returns when this action occurs during a turn.
    ///
    /// @return the timing point for this action
    TurnBasedTiming timing();

    /// Executes this turn-based action.
    ///
    /// The action should create appropriate game events and process them
    /// through the provided processor to ensure replacement effects and
    /// triggers are handled correctly.
    ///
    /// @param state the current game state
    /// @param processor the event processor for game state changes
    void execute(GameState state, GameEventProcessor processor);
}
