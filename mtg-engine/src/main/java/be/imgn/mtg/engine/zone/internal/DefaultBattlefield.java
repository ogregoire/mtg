package be.imgn.mtg.engine.zone.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import be.imgn.mtg.engine.characteristics.Type;
import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.object.Card;
import be.imgn.mtg.engine.object.ObjectId;
import be.imgn.mtg.engine.object.Permanent;
import be.imgn.mtg.engine.object.Token;
import be.imgn.mtg.engine.zone.Battlefield;

/// Default implementation of [Battlefield].
///
/// Maintains multiple indexes for efficient lookup by ID, controller, and type.
public final class DefaultBattlefield extends AbstractZone<Permanent> implements Battlefield {

    /// Index of permanents by controller for efficient lookup.
    private final Map<Player, List<Permanent>> byController = new HashMap<>();

    /// Creates a new empty battlefield.
    public DefaultBattlefield() {}

    @Override
    public Permanent enter(Card card, Player controller) {
        var permanent = Permanent.fromCard(card, controller).build();
        enter(permanent);
        return permanent;
    }

    @Override
    public Permanent enter(Token token, Player controller) {
        var permanent = Permanent.fromToken(token, controller).build();
        enter(permanent);
        return permanent;
    }

    @Override
    public void enter(Permanent permanent) {
        index(permanent);
        byController
                .computeIfAbsent(permanent.controller(), k -> new ArrayList<>())
                .add(permanent);
    }

    @Override
    public Optional<Permanent> remove(ObjectId id) {
        var permanent = unindex(id);
        if (permanent != null) {
            var controllerList = byController.get(permanent.controller());
            if (controllerList != null) {
                controllerList.remove(permanent);
            }
        }
        return Optional.ofNullable(permanent);
    }

    @Override
    public List<Permanent> controlledBy(Player controller) {
        var list = byController.get(controller);
        return list != null ? List.copyOf(list) : List.of();
    }

    @Override
    public List<Permanent> ofType(Type type) {
        return objectsById.values().stream()
                .filter(p -> p.types().contains(type))
                .toList();
    }

    @Override
    public List<Permanent> controlledByOfType(Player controller, Type type) {
        var list = byController.get(controller);
        if (list == null) {
            return List.of();
        }
        return list.stream().filter(p -> p.types().contains(type)).toList();
    }

    @Override
    public List<Permanent> all() {
        return List.copyOf(objectsById.values());
    }

    @Override
    public Stream<Permanent> stream() {
        return objectsById.values().stream();
    }
}
