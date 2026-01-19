package be.imgn.mtg.engine.state.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import be.imgn.mtg.engine.object.ObjectId;
import be.imgn.mtg.engine.state.LastKnownInformation;
import be.imgn.mtg.engine.state.ObjectSnapshot;

/// Default implementation of [LastKnownInformation].
///
/// Uses a simple map to store snapshots keyed by object ID.
public final class DefaultLastKnownInformation implements LastKnownInformation {

    private final Map<ObjectId, ObjectSnapshot> snapshots = new HashMap<>();

    /// Creates a new empty LKI tracker.
    public DefaultLastKnownInformation() {}

    @Override
    public void record(ObjectSnapshot snapshot) {
        snapshots.put(snapshot.id(), snapshot);
    }

    @Override
    public Optional<ObjectSnapshot> get(ObjectId id) {
        return Optional.ofNullable(snapshots.get(id));
    }

    @Override
    public void clear() {
        snapshots.clear();
    }

    @Override
    public void clear(ObjectId id) {
        snapshots.remove(id);
    }
}
