package be.imgn.mtg.engine.zone.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.object.Card;
import be.imgn.mtg.engine.object.GameObject;
import be.imgn.mtg.engine.object.ObjectId;
import be.imgn.mtg.engine.zone.CommandZone;

/// Default implementation of [CommandZone].
///
/// Stores commanders indexed by owner.
public final class DefaultCommandZone implements CommandZone {

    /// Map from object ID to commander card.
    private final Map<ObjectId, Card> commandersById = new HashMap<>();

    /// Map from player to their commanders.
    private final Map<Player, List<Card>> commandersByOwner = new HashMap<>();

    /// Creates a new empty command zone.
    public DefaultCommandZone() {}

    @Override
    public void addCommander(Card commander, Player owner) {
        commandersById.put(commander.id(), commander);
        commandersByOwner.computeIfAbsent(owner, k -> new ArrayList<>()).add(commander);
    }

    @Override
    public List<Card> commanders(Player owner) {
        var list = commandersByOwner.get(owner);
        return list != null ? List.copyOf(list) : List.of();
    }

    @Override
    public Optional<Card> removeCommander(ObjectId id) {
        var commander = commandersById.remove(id);
        if (commander != null) {
            // Find and remove from owner's list
            for (var entry : commandersByOwner.entrySet()) {
                if (entry.getValue().remove(commander)) {
                    break;
                }
            }
        }
        return Optional.ofNullable(commander);
    }

    @Override
    public List<Card> allCommanders() {
        return List.copyOf(commandersById.values());
    }

    @Override
    public List<GameObject> all() {
        return new ArrayList<>(commandersById.values());
    }

    @Override
    public int size() {
        return commandersById.size();
    }

    @Override
    public boolean isEmpty() {
        return commandersById.isEmpty();
    }

    @Override
    public boolean contains(ObjectId id) {
        return commandersById.containsKey(id);
    }

    @Override
    public Optional<GameObject> findById(ObjectId id) {
        return Optional.ofNullable(commandersById.get(id));
    }

    @Override
    public Stream<GameObject> stream() {
        return commandersById.values().stream().map(c -> c);
    }
}
