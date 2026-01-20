package be.imgn.mtg.engine.turn;

import java.util.Optional;

import be.imgn.mtg.engine.game.Player;

/// Tracks the current turn state including turn order and active player ({@mtg.rule 500}).
///
/// The turn state maintains:
/// - The current turn number
/// - The current active player
/// - The turn order (which player goes next)
/// - Extra turns queue (LIFO - last in, first out)
///
/// @see TurnTracker
public interface TurnState {

    /// Returns the current turn number (1-indexed).
    ///
    /// @return the current turn number
    int turnNumber();

    /// Returns the current active player ({@mtg.rule 102.1}).
    ///
    /// The active player is the player whose turn it is.
    ///
    /// @return the active player
    Player activePlayer();

    /// Advances to the next turn and returns the new active player.
    ///
    /// If there are extra turns queued, the next extra turn is taken.
    /// Otherwise, the next player in turn order becomes active.
    ///
    /// @return the player whose turn it now is
    Player nextTurn();

    /// Removes a player from the turn order ({@mtg.rule 800.4}).
    ///
    /// When a player leaves the game, they are removed from the turn order.
    /// If they had any pending extra turns, those are also removed.
    ///
    /// @param player the player to remove
    void removePlayer(Player player);

    /// Queues an extra turn for a player ({@mtg.rule 500.7}).
    ///
    /// Extra turns are stored in a LIFO queue. When multiple players
    /// get extra turns simultaneously, APNAP order determines the order
    /// they're added to the queue.
    ///
    /// @param player the player who gets the extra turn
    void addExtraTurn(Player player);

    /// Returns the player who will take the next extra turn, if any.
    ///
    /// @return the player for the next extra turn, or empty if no extra turns queued
    Optional<Player> peekExtraTurn();
}
