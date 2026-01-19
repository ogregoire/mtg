package be.imgn.mtg.engine.zone;

import java.util.Optional;
import java.util.stream.Stream;

import be.imgn.mtg.engine.object.ObjectId;

/// Base interface for all zones in Magic: The Gathering ({@mtg.rule 400}).
///
/// A zone is a place where objects can exist during a game. There are normally seven zones:
/// library, hand, battlefield, graveyard, stack, exile, and command.
///
/// Zones are data structures that store game objects. The orchestration of zone changes
/// (with replacement effects, last known information, and events) is handled by the
/// GameActionProcessor, not by zones directly.
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

    /// Returns true if this zone contains an object with the given ID.
    ///
    /// @param id the object ID to check
    /// @return true if found
    boolean contains(ObjectId id);

    /// Finds an object in this zone by its ID.
    ///
    /// @param id the object ID to find
    /// @return the object, or empty if not found
    Optional<T> findById(ObjectId id);

    /// Returns a stream of all objects in this zone.
    ///
    /// For ordered zones (library, graveyard, stack), the stream preserves order.
    /// For unordered zones, the iteration order is not guaranteed.
    ///
    /// @return a stream of objects
    Stream<T> stream();
}
