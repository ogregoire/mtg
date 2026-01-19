package be.imgn.mtg.engine.zone;

import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.object.GameObject;

/// Event representing a game object being put into a graveyard generically.
///
/// This covers cases that aren't discard, mill, or dies:
/// - Instant/sorcery spells resolving
/// - Permanents being sacrificed without being "destroyed"
/// - Cards being put into graveyard from exile or other zones
///
/// For more specific events, see [DiscardEvent], [MillEvent], and [DiesEvent].
///
/// @param object the object being put into the graveyard
/// @param from the zone it's coming from
public record PutIntoGraveyardEvent(GameObject object, ZoneType from) implements ZoneChangeEvent {

    @Override
    public ZoneType to() {
        return ZoneType.GRAVEYARD;
    }

    @Override
    public Player affectedPlayer() {
        return object.owner();
    }
}
