package be.imgn.mtg.engine.state.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import be.imgn.mtg.engine.object.GameObject;
import be.imgn.mtg.engine.state.LocatedObject;
import be.imgn.mtg.engine.zone.Zone;

/// Central storage for all game objects across all zones.
///
/// Zone implementations delegate their storage to this store.
/// The GameState exposes the store's contents through [be.imgn.mtg.engine.state.GameState#objects()].
///
/// All comparisons use identity (`==`), matching the existing [Zone#containsObject] semantics.
public final class ObjectStore {

    private final List<LocatedObject> objects = new ArrayList<>();

    /// Adds an object to the store in the given zone.
    ///
    /// @param object the game object to add
    /// @param zone the zone the object resides in
    public void add(GameObject object, Zone<?> zone) {
        objects.add(new LocatedObject(object, zone));
    }

    /// Removes an object from the store.
    ///
    /// @param object the game object to remove
    /// @return true if the object was found and removed
    public boolean remove(GameObject object) {
        return objects.removeIf(lo -> lo.object() == object);
    }

    /// Returns whether the store contains the given object in the given zone.
    ///
    /// @param object the game object to check
    /// @param zone the zone to check
    /// @return true if found
    public boolean contains(GameObject object, Zone<?> zone) {
        return objects.stream().anyMatch(lo -> lo.object() == object && lo.zone() == zone);
    }

    /// Returns the number of objects in the given zone.
    ///
    /// @param zone the zone
    /// @return the count
    public int count(Zone<?> zone) {
        return (int) objects.stream().filter(lo -> lo.zone() == zone).count();
    }

    /// Returns whether the given zone is empty.
    ///
    /// @param zone the zone
    /// @return true if the zone has no objects
    public boolean isEmpty(Zone<?> zone) {
        return objects.stream().noneMatch(lo -> lo.zone() == zone);
    }

    /// Returns a typed stream of objects in the given zone.
    ///
    /// @param zone the zone to stream
    /// @param type the expected element type
    /// @param <T> the element type
    /// @return a stream of objects in the zone
    public <T> Stream<T> stream(Zone<?> zone, Class<T> type) {
        return objects.stream().filter(lo -> lo.zone() == zone).map(lo -> type.cast(lo.object()));
    }

    /// Returns a stream of all located objects.
    ///
    /// @return a stream of all objects with their zones
    public Stream<LocatedObject> stream() {
        return objects.stream();
    }
}
