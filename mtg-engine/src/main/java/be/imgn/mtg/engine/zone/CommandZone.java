package be.imgn.mtg.engine.zone;

import java.util.List;

import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.object.Card;
import be.imgn.mtg.engine.object.GameObject;

/// The command zone - for commanders and emblems ({@mtg.rule 408}).
///
/// The command zone is a shared zone where certain objects exist that affect the game
/// but aren't on the battlefield. This includes commanders (in Commander format) and
/// emblems created by planeswalkers.
///
/// Commanders can be cast from the command zone and return there when they would
/// go to another zone (with owner permission). Emblems remain in the command zone
/// for the entire game.
///
/// @see ZoneType#COMMAND
public non-sealed interface CommandZone extends Zone<GameObject> {

    /// Adds a commander to the command zone.
    ///
    /// @param commander the commander card
    /// @param owner the player who owns the commander
    void addCommander(Card commander, Player owner);

    /// Returns a player's commander(s).
    ///
    /// @param owner the player
    /// @return the player's commanders
    List<Card> commanders(Player owner);

    /// Removes a commander from the command zone (when cast or moved).
    ///
    /// @param commander the commander card to remove
    /// @return true if the commander was found and removed
    boolean removeCommander(Card commander);

    /// Returns all commanders in the command zone.
    ///
    /// @return all commanders
    List<Card> allCommanders();

    /// Returns all objects in the command zone.
    ///
    /// This includes commanders and any other objects (like emblems when implemented).
    ///
    /// @return all objects
    List<GameObject> all();

    @Override
    default ZoneType type() {
        return ZoneType.COMMAND;
    }
}
