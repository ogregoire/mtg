package be.imgn.mtg.engine.zone;

import java.util.stream.Stream;

import be.imgn.mtg.engine.object.GameObject;

/// Base interface for all zones in Magic: The Gathering ({@mtg.rule 400}).
///
/// A zone is a place where objects can exist during a game. There are normally seven zones:
/// library, hand, battlefield, graveyard, stack, exile, and command.
///
/// Zones are data structures that store game objects. The orchestration of zone changes
/// (with replacement effects, last known information, and events) is handled by the
/// GameEventProcessor, not by zones directly.
///
/// @param <T> the type of objects stored in this zone
/// @see ZoneType
/// @see Library
/// @see Hand
/// @see Battlefield
/// @see Graveyard
/// @see Stack
/// @see Exile
/// @see CommandZone
public sealed interface Zone<T> permits Library, Hand, Battlefield, Graveyard, Stack, Exile, CommandZone {

    /// Returns the type of this zone.
    ///
    /// @return the zone type, never null
    ZoneType type();

    /// Returns the number of objects in this zone.
    ///
    /// @return the count of objects
    int size();

    /// Returns true if this zone contains no objects.
    ///
    /// @return true if empty
    boolean isEmpty();

    /// Returns true if this zone contains the given object.
    ///
    /// @param object the object to check
    /// @return true if found
    boolean contains(T object);

    /// Returns true if this zone contains the given game object.
    ///
    /// This method accepts any [GameObject] without requiring a cast to the zone's element type.
    /// Useful when the caller doesn't know the specific zone type.
    ///
    /// @param object the game object to check
    /// @return true if found
    default boolean containsObject(GameObject object) {
        return stream().anyMatch(obj -> obj == object);
    }

    /// Returns a stream of all objects in this zone.
    ///
    /// For ordered zones (library, graveyard, stack), the stream preserves order.
    /// For unordered zones, the iteration order is not guaranteed.
    ///
    /// @return a stream of objects
    Stream<T> stream();
}
