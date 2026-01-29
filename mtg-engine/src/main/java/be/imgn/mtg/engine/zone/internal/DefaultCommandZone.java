package be.imgn.mtg.engine.zone.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.object.Card;
import be.imgn.mtg.engine.object.GameObject;
import be.imgn.mtg.engine.zone.CommandZone;

/// Default implementation of [CommandZone].
///
/// Stores commanders indexed by owner.
public final class DefaultCommandZone implements CommandZone {

    /// Set of commander cards.
    private final Set<Card> commanders = new HashSet<>();

    /// Map from player to their commanders.
    private final Map<Player, List<Card>> commandersByOwner = new HashMap<>();

    /// Creates a new empty command zone.
    public DefaultCommandZone() {}

    @Override
    public void addCommander(Card commander, Player owner) {
        commanders.add(commander);
        commandersByOwner.computeIfAbsent(owner, k -> new ArrayList<>()).add(commander);
    }

    @Override
    public List<Card> commanders(Player owner) {
        var list = commandersByOwner.get(owner);
        return list != null ? List.copyOf(list) : List.of();
    }

    @Override
    public boolean removeCommander(Card commander) {
        if (commanders.remove(commander)) {
            for (var entry : commandersByOwner.entrySet()) {
                if (entry.getValue().remove(commander)) {
                    break;
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public List<Card> allCommanders() {
        return List.copyOf(commanders);
    }

    @Override
    public List<GameObject> all() {
        return new ArrayList<>(commanders);
    }

    @Override
    public int size() {
        return commanders.size();
    }

    @Override
    public boolean isEmpty() {
        return commanders.isEmpty();
    }

    @Override
    public boolean contains(GameObject object) {
        return commanders.contains(object);
    }

    @Override
    public boolean containsObject(GameObject object) {
        return commanders.contains(object);
    }

    @Override
    public Stream<GameObject> stream() {
        return commanders.stream().map(c -> c);
    }
}
