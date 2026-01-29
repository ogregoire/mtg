package be.imgn.mtg.engine.state.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import be.imgn.mtg.engine.object.GameObject;
import be.imgn.mtg.engine.state.LastKnownInformation;
import be.imgn.mtg.engine.state.ObjectSnapshot;

/// Default implementation of [LastKnownInformation].
///
/// Uses a simple map to store snapshots keyed by game object reference.
public final class DefaultLastKnownInformation implements LastKnownInformation {

    private final Map<GameObject, ObjectSnapshot> snapshots = new HashMap<>();

    /// Creates a new empty LKI tracker.
    public DefaultLastKnownInformation() {}

    @Override
    public void record(GameObject object, ObjectSnapshot snapshot) {
        snapshots.put(object, snapshot);
    }

    @Override
    public Optional<ObjectSnapshot> get(GameObject object) {
        return Optional.ofNullable(snapshots.get(object));
    }

    @Override
    public void clear() {
        snapshots.clear();
    }

    @Override
    public void clear(GameObject object) {
        snapshots.remove(object);
    }
}
