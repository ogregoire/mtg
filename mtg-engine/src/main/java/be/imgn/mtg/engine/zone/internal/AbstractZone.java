package be.imgn.mtg.engine.zone.internal;

import java.util.HashSet;
import java.util.Set;

import be.imgn.mtg.engine.object.GameObject;

/// Abstract base class for zone implementations.
///
/// Provides common storage and lookup functionality using a set of objects.
/// Does not implement Zone directly (Zone is sealed); concrete implementations
/// should implement the specific zone interface.
///
/// @param <T> the type of game object stored in this zone
abstract class AbstractZone<T extends GameObject> {

    /// Set of objects for efficient lookup using reference identity.
    protected final Set<T> objects = new HashSet<>();

    public int size() {
        return objects.size();
    }

    public boolean isEmpty() {
        return objects.isEmpty();
    }

    public boolean contains(T object) {
        return objects.contains(object);
    }

    public boolean containsObject(GameObject object) {
        return objects.contains(object);
    }

    /// Adds an object to the internal index.
    ///
    /// @param object the object to index
    protected void index(T object) {
        objects.add(object);
    }

    /// Removes an object from the internal index.
    ///
    /// @param object the object to remove
    /// @return true if the object was found and removed
    protected boolean unindex(T object) {
        return objects.remove(object);
    }
}
