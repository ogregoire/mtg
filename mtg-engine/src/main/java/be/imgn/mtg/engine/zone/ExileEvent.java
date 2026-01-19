package be.imgn.mtg.engine.zone;

import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.object.GameObject;

/// Event representing a game object being exiled ({@mtg.rule 406}).
///
/// Exile is a zone where cards are placed face up, typically removed from the game
/// but potentially able to return. Objects can be exiled from any zone.
///
/// @param object the object being exiled (card or permanent)
/// @param from the zone the object is being exiled from
/// @param controller the player who controls/owns the object
public record ExileEvent(GameObject object, ZoneType from, Player controller) implements ZoneChangeEvent {

    @Override
    public ZoneType to() {
        return ZoneType.EXILE;
    }

    @Override
    public Player affectedPlayer() {
        return controller;
    }
}
