package be.imgn.mtg.engine.state;

import java.util.List;
import java.util.Optional;

import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.object.GameObject;
import be.imgn.mtg.engine.result.GameResult;
import be.imgn.mtg.engine.zone.Battlefield;
import be.imgn.mtg.engine.zone.CommandZone;
import be.imgn.mtg.engine.zone.Exile;
import be.imgn.mtg.engine.zone.Graveyard;
import be.imgn.mtg.engine.zone.Hand;
import be.imgn.mtg.engine.zone.Library;
import be.imgn.mtg.engine.zone.Stack;
import be.imgn.mtg.engine.zone.Zone;

/// Central game state aggregating all zones and providing object lookup.
///
/// The GameState provides a unified view of all game zones. Shared zones (battlefield,
/// stack, exile, command zone) are directly accessible. Per-player zones (library, hand,
/// graveyard) are accessed by providing the player.
///
/// This is the primary entry point for accessing game objects and their locations.
///
/// @see Zone
/// @see LastKnownInformation
public interface GameState {

    // --- Shared zones ---

    /// Returns the battlefield zone.
    ///
    /// @return the battlefield
    Battlefield battlefield();

    /// Returns the stack zone.
    ///
    /// @return the stack
    Stack stack();

    /// Returns the exile zone.
    ///
    /// @return the exile zone
    Exile exile();

    /// Returns the command zone.
    ///
    /// @return the command zone
    CommandZone commandZone();

    // --- Per-player zones ---

    /// Returns the library for a specific player.
    ///
    /// @param player the player
    /// @return the player's library
    Library library(Player player);

    /// Returns the hand for a specific player.
    ///
    /// @param player the player
    /// @return the player's hand
    Hand hand(Player player);

    /// Returns the graveyard for a specific player.
    ///
    /// @param player the player
    /// @return the player's graveyard
    Graveyard graveyard(Player player);

    // --- Object lookup ---

    /// Finds which zone contains the given game object.
    ///
    /// @param object the game object to find
    /// @return the zone containing the object, or empty if not found
    Optional<Zone<?>> findZone(GameObject object);

    // --- Last known information ---

    /// Returns the last known information tracker.
    ///
    /// @return the LKI tracker
    LastKnownInformation lastKnownInformation();

    // --- Active player and turn tracking ---

    /// Returns the current active player ({@mtg.rule 102.1}).
    ///
    /// The active player is the player whose turn it is.
    ///
    /// @return the active player
    Player activePlayer();

    /// Sets the current active player.
    ///
    /// @param player the new active player
    void setActivePlayer(Player player);

    /// Returns all players in the game in turn order.
    ///
    /// @return an unmodifiable list of players in turn order
    List<Player> players();

    /// Returns the next player in turn order after the given player.
    ///
    /// @param current the current player
    /// @return the next player in turn order
    Player nextPlayerInTurnOrder(Player current);

    // --- Game end tracking ---

    /// Returns whether the game has ended.
    ///
    /// @return true if the game is over
    boolean isGameOver();

    /// Returns the game result if the game has ended.
    ///
    /// @return the game result, or empty if the game is still ongoing
    Optional<GameResult> getResult();

    /// Sets the game result, marking the game as over.
    ///
    /// @param result the result of the game
    void setResult(GameResult result);

    // --- Mana pool management ---

    /// Empties the mana pools of all players ({@mtg.rule 500.4}).
    ///
    /// This happens at the end of each step and phase.
    void emptyManaPools();
}
