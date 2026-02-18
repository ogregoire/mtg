package be.imgn.mtg.engine.state;

import be.imgn.mtg.engine.object.GameObject;
import be.imgn.mtg.engine.zone.Zone;

/// A game object paired with the zone it currently resides in.
///
/// Returned by [GameState#objects()] to provide zone context alongside each object.
///
/// @param object the game object
/// @param zone the zone the object is in
public record LocatedObject(GameObject object, Zone<?> zone) {}
