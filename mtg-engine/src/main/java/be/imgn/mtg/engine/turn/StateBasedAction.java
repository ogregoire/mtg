package be.imgn.mtg.engine.turn;

import be.imgn.mtg.engine.state.GameState;

/// A single state-based action ({@mtg.rule 704}).
///
/// State-based actions are automatic game actions that happen whenever
/// certain conditions are met. They don't use the stack and no player
/// receives priority while they're being processed.
///
/// @see SBAEngine
public interface StateBasedAction {

    /// Checks if this state-based action currently applies to the game state.
    ///
    /// @param gameState the current game state
    /// @return true if this SBA's condition is met and it should be applied
    boolean appliesTo(GameState gameState);

    /// Applies this state-based action to the game state.
    ///
    /// This method should only be called if appliesTo() returns true.
    /// The action modifies the game state directly (e.g., destroying a creature,
    /// causing a player to lose).
    ///
    /// @param gameState the game state to modify
    void apply(GameState gameState);
}
