package be.imgn.mtg.engine.turn;

import be.imgn.mtg.engine.state.GameState;

/// Checks and applies state-based actions ({@mtg.rule 704}).
///
/// State-based actions (SBAs) are game actions that happen automatically
/// whenever certain conditions are met. The SBA engine checks all SBAs
/// repeatedly until none apply, as one SBA can cause conditions for another.
///
/// @see StateBasedAction
public interface SBAEngine {

    /// Checks and applies all applicable state-based actions.
    ///
    /// This method repeatedly checks all registered SBAs and applies any
    /// that match. It continues until no SBAs apply in a complete pass.
    /// Triggered abilities that result from SBAs are not put on the stack
    /// until all SBAs have finished being applied ({@mtg.rule 704.3}).
    ///
    /// @param gameState the current game state
    /// @return true if any state-based actions were applied
    boolean checkAndApply(GameState gameState);

    /// Checks if any state-based actions would apply without applying them.
    ///
    /// This is used to determine if cleanup needs to repeat ({@mtg.rule 514.3a}).
    ///
    /// @param gameState the current game state
    /// @return true if any SBAs would apply
    boolean wouldPerformActions(GameState gameState);
}
