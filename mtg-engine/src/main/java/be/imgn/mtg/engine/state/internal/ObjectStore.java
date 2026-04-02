package be.imgn.mtg.engine.state.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.object.Card;
import be.imgn.mtg.engine.object.GameObject;
import be.imgn.mtg.engine.object.Permanent;
import be.imgn.mtg.engine.state.LocatedObject;
import be.imgn.mtg.engine.util.ListMultimap;
import be.imgn.mtg.engine.zone.Zone;

/// Central storage for all game objects across all zones.
///
/// Zone implementations delegate their storage to this store.
/// The GameState exposes the store's contents through [be.imgn.mtg.engine.state.GameState#objects()].
///
/// All comparisons use identity (`==`), matching the existing [Zone#containsObject] semantics.
public final class ObjectStore {

    /// Primary index: zone → objects in that zone.
    private final Map<Zone<?>, List<GameObject>> byZone = new IdentityHashMap<>();

    /// Reverse index: object → zone it resides in.
    private final IdentityHashMap<GameObject, Zone<?>> zoneOf = new IdentityHashMap<>();

    /// Battlefield secondary index: controller → permanents.
    private final ListMultimap<Player, Permanent> byController = ListMultimap.newHashListMultimap();

    /// Exile secondary index: face-down objects.
    private final Set<GameObject> faceDown = Collections.newSetFromMap(new IdentityHashMap<>());

    /// Command zone secondary index: owner → commanders.
    private final ListMultimap<Player, Card> commandersByOwner = ListMultimap.newHashListMultimap();

    /// Adds an object to the store in the given zone.
    ///
    /// @param object the game object to add
    /// @param zone the zone the object resides in
    public void add(GameObject object, Zone<?> zone) {
        byZone.computeIfAbsent(zone, _ -> new ArrayList<>()).add(object);
        zoneOf.put(object, zone);
        if (object instanceof Permanent p) {
            byController.put(p.controller(), p);
        }
    }

    /// Removes an object from the store.
    ///
    /// @param object the game object to remove
    /// @return true if the object was found and removed
    public boolean remove(GameObject object) {
        var zone = zoneOf.remove(object);
        if (zone == null) {
            return false;
        }
        var list = byZone.get(zone);
        if (list != null) {
            list.remove(object);
        }
        if (object instanceof Permanent p) {
            byController.remove(p.controller(), p);
        }
        faceDown.remove(object);
        return true;
    }

    /// Returns whether the store contains the given object in the given zone.
    ///
    /// @param object the game object to check
    /// @param zone the zone to check
    /// @return true if found
    public boolean contains(GameObject object, Zone<?> zone) {
        return zoneOf.get(object) == zone;
    }

    /// Returns the number of objects in the given zone.
    ///
    /// @param zone the zone
    /// @return the count
    public int count(Zone<?> zone) {
        var list = byZone.get(zone);
        return list == null ? 0 : list.size();
    }

    /// Returns whether the given zone is empty.
    ///
    /// @param zone the zone
    /// @return true if the zone has no objects
    public boolean isEmpty(Zone<?> zone) {
        var list = byZone.get(zone);
        return list == null || list.isEmpty();
    }

    /// Returns a typed stream of objects in the given zone.
    ///
    /// @param zone the zone to stream
    /// @param type the expected element type
    /// @param <T> the element type
    /// @return a stream of objects in the zone
    public <T> Stream<T> stream(Zone<?> zone, Class<T> type) {
        var list = byZone.get(zone);
        if (list == null) {
            return Stream.empty();
        }
        return list.stream().map(type::cast);
    }

    /// Returns a stream of all located objects.
    ///
    /// @return a stream of all objects with their zones
    public Stream<LocatedObject> stream() {
        return zoneOf.entrySet().stream().map(e -> new LocatedObject(e.getKey(), e.getValue()));
    }

    /// Finds the zone containing the given object.
    ///
    /// @param object the game object to locate
    /// @return the zone, or empty if not found
    public Optional<Zone<?>> findZone(GameObject object) {
        return Optional.ofNullable(zoneOf.get(object));
    }

    /// Marks an object as face-down.
    ///
    /// @param object the object to mark
    public void markFaceDown(GameObject object) {
        faceDown.add(object);
    }

    /// Returns whether an object is face-down.
    ///
    /// @param object the object to check
    /// @return true if face-down
    public boolean isFaceDown(GameObject object) {
        return faceDown.contains(object);
    }

    /// Returns all permanents controlled by the given player.
    ///
    /// @param controller the controlling player
    /// @return an unmodifiable list of permanents
    public List<Permanent> permanentsByController(Player controller) {
        return List.copyOf(byController.get(controller));
    }

    /// Registers a commander card with its owner.
    ///
    /// @param commander the commander card
    /// @param owner the owning player
    public void addCommander(Card commander, Player owner) {
        commandersByOwner.put(owner, commander);
    }

    /// Returns all commanders owned by the given player.
    ///
    /// @param owner the owning player
    /// @return an unmodifiable list of commander cards
    public List<Card> commanders(Player owner) {
        return List.copyOf(commandersByOwner.get(owner));
    }

    /// Returns all commanders across all players.
    ///
    /// @return an unmodifiable list of all commander cards
    public List<Card> allCommanders() {
        return commandersByOwner.values().stream().toList();
    }

    /// Removes a commander from the owner index.
    ///
    /// @param commander the commander card to remove
    public void removeCommander(Card commander) {
        for (var key : commandersByOwner.keySet()) {
            if (commandersByOwner.remove(key, commander)) {
                break;
            }
        }
    }
}
