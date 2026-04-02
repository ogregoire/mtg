package be.imgn.mtg.engine.zone.internal;

import java.util.List;
import java.util.stream.Stream;

import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.object.Card;
import be.imgn.mtg.engine.object.GameObject;
import be.imgn.mtg.engine.state.internal.ObjectStore;
import be.imgn.mtg.engine.zone.CommandZone;

/// Default implementation of [CommandZone].
///
/// Backed by the central [ObjectStore]. Commander ownership is tracked by the store.
public final class DefaultCommandZone implements CommandZone {

    private final ObjectStore store;

    /// Creates a new empty command zone backed by the given store.
    ///
    /// @param store the central object store
    public DefaultCommandZone(ObjectStore store) {
        this.store = store;
    }

    @Override
    public void addCommander(Card commander, Player owner) {
        store.add(commander, this);
        store.addCommander(commander, owner);
    }

    @Override
    public List<Card> commanders(Player owner) {
        return store.commanders(owner);
    }

    @Override
    public boolean removeCommander(Card commander) {
        if (!store.remove(commander)) {
            return false;
        }
        store.removeCommander(commander);
        return true;
    }

    @Override
    public List<Card> allCommanders() {
        return store.allCommanders();
    }

    @Override
    public List<GameObject> all() {
        return stream().toList();
    }

    @Override
    public int size() {
        return store.count(this);
    }

    @Override
    public boolean isEmpty() {
        return store.isEmpty(this);
    }

    @Override
    public boolean contains(GameObject object) {
        return store.contains(object, this);
    }

    @Override
    public boolean containsObject(GameObject object) {
        return store.contains(object, this);
    }

    @Override
    public Stream<GameObject> stream() {
        return store.stream(this, GameObject.class);
    }
}
