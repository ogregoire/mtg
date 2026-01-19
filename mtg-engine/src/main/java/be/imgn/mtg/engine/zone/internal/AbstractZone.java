package be.imgn.mtg.engine.zone.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import be.imgn.mtg.engine.object.GameObject;
import be.imgn.mtg.engine.object.ObjectId;

/// Abstract base class for zone implementations.
///
/// Provides common storage and lookup functionality using an ID-to-object map.
/// Does not implement Zone directly (Zone is sealed); concrete implementations
/// should implement the specific zone interface.
///
/// @param <T> the type of game object stored in this zone
abstract class AbstractZone<T extends GameObject> {

    /// Map from object ID to object for efficient lookup.
    protected final Map<ObjectId, T> objectsById = new HashMap<>();

    public int size() {
        return objectsById.size();
    }

    public boolean isEmpty() {
        return objectsById.isEmpty();
    }

    public boolean contains(ObjectId id) {
        return objectsById.containsKey(id);
    }

    public Optional<T> findById(ObjectId id) {
        return Optional.ofNullable(objectsById.get(id));
    }

    /// Adds an object to the internal index.
    ///
    /// @param object the object to index
    protected void index(T object) {
        objectsById.put(object.id(), object);
    }

    /// Removes an object from the internal index.
    ///
    /// @param id the ID of the object to remove
    /// @return the removed object, or null if not found
    protected T unindex(ObjectId id) {
        return objectsById.remove(id);
    }
}
