package be.imgn.mtg.engine.state;

import java.util.Optional;

import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.object.GameObject;
import be.imgn.mtg.engine.object.ObjectId;
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

    /// Finds a game object by its ID, searching all zones.
    ///
    /// @param id the object ID to find
    /// @return the object, or empty if not found in any zone
    Optional<GameObject> findObject(ObjectId id);

    /// Finds which zone contains an object with the given ID.
    ///
    /// @param id the object ID to find
    /// @return the zone containing the object, or empty if not found
    Optional<Zone<?>> findZone(ObjectId id);

    // --- Last known information ---

    /// Returns the last known information tracker.
    ///
    /// @return the LKI tracker
    LastKnownInformation lastKnownInformation();
}
