package be.imgn.mtg.engine.turn;

import java.util.concurrent.CompletableFuture;

import be.imgn.mtg.engine.game.Player;

/// Handles player input during priority passes ({@mtg.rule 117}).
///
/// This interface abstracts how player decisions are obtained, allowing
/// for different implementations (AI, network, UI) without changing the
/// turn system logic.
///
/// @see PlayerAction
/// @see PrioritySystem
public interface PlayerInputHandler {

    /// Waits for a player to choose an action.
    ///
    /// This method returns a future that completes when the player makes
    /// a decision. Implementations may block, poll a UI, or receive input
    /// from a network connection.
    ///
    /// @param player the player who must choose an action
    /// @return a future that completes with the player's chosen action
    CompletableFuture<PlayerAction> waitForAction(Player player);
}
