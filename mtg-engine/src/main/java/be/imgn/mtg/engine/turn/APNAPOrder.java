package be.imgn.mtg.engine.turn;

import java.util.List;

import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.state.GameState;

/// Determines Active Player, Non-Active Player (APNAP) order ({@mtg.rule 101.4}).
///
/// APNAP order is used for simultaneous choices and effects. The active player
/// makes choices first, followed by each other player in turn order.
///
/// @see PrioritySystem
public interface APNAPOrder {

    /// Returns all players in APNAP order based on the current active player.
    ///
    /// @param gameState the current game state
    /// @return players in APNAP order (active player first)
    List<Player> getOrder(GameState gameState);

    /// Returns all players in APNAP order with a specific active player.
    ///
    /// This is used when the active player for APNAP order differs from
    /// the current turn's active player (rare edge cases).
    ///
    /// @param gameState the current game state
    /// @param activePlayer the player to treat as active for APNAP order
    /// @return players in APNAP order starting with the specified player
    List<Player> getOrder(GameState gameState, Player activePlayer);
}
