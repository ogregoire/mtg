package be.imgn.mtg.engine.zone.internal;

import java.util.List;
import java.util.stream.Stream;

import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.object.Card;
import be.imgn.mtg.engine.object.GameObject;
import be.imgn.mtg.engine.state.internal.ObjectStore;
import be.imgn.mtg.engine.util.ListMultimap;
import be.imgn.mtg.engine.zone.CommandZone;

/// Default implementation of [CommandZone].
///
/// Backed by the central [ObjectStore]. Maintains a secondary index by owner
/// for commander lookup.
public final class DefaultCommandZone implements CommandZone {

    private final ObjectStore store;
    private final ListMultimap<Player, Card> commandersByOwner = ListMultimap.newHashListMultimap();

    /// Creates a new empty command zone backed by the given store.
    ///
    /// @param store the central object store
    public DefaultCommandZone(ObjectStore store) {
        this.store = store;
    }

    @Override
    public void addCommander(Card commander, Player owner) {
        store.add(commander, this);
        commandersByOwner.put(owner, commander);
    }

    @Override
    public List<Card> commanders(Player owner) {
        return List.copyOf(commandersByOwner.get(owner));
    }

    @Override
    public boolean removeCommander(Card commander) {
        if (!store.remove(commander)) {
            return false;
        }
        for (var key : commandersByOwner.keySet()) {
            if (commandersByOwner.remove(key, commander)) {
                break;
            }
        }
        return true;
    }

    @Override
    public List<Card> allCommanders() {
        return commandersByOwner.values().stream().toList();
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
