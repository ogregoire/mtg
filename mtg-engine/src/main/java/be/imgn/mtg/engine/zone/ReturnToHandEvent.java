package be.imgn.mtg.engine.zone;

import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.object.GameObject;

/// Event representing a game object being returned to its owner's hand.
///
/// This includes bouncing permanents from the battlefield and returning cards
/// from the graveyard, exile, or other zones to hand.
///
/// @param object the object being returned (card or permanent)
/// @param from the zone it's being returned from
public record ReturnToHandEvent(GameObject object, ZoneType from) implements ZoneChangeEvent {

    @Override
    public ZoneType to() {
        return ZoneType.HAND;
    }

    @Override
    public Player affectedPlayer() {
        return object.owner();
    }
}
