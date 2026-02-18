package be.imgn.mtg.engine.zone.internal;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import be.imgn.mtg.engine.characteristics.Type;
import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.object.Card;
import be.imgn.mtg.engine.object.GameObject;
import be.imgn.mtg.engine.object.Permanent;
import be.imgn.mtg.engine.object.Token;
import be.imgn.mtg.engine.state.internal.ObjectStore;
import be.imgn.mtg.engine.util.ListMultimap;
import be.imgn.mtg.engine.zone.Battlefield;

/// Default implementation of [Battlefield].
///
/// Backed by the central [ObjectStore]. Maintains a secondary index by controller
/// for efficient lookup.
public final class DefaultBattlefield implements Battlefield {

    private final ObjectStore store;
    private final ListMultimap<Player, Permanent> byController = ListMultimap.newHashListMultimap();

    /// Creates a new empty battlefield backed by the given store.
    ///
    /// @param store the central object store
    public DefaultBattlefield(ObjectStore store) {
        this.store = store;
    }

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
        store.add(permanent, this);
        byController.put(permanent.controller(), permanent);
    }

    @Override
    public boolean remove(Permanent permanent) {
        if (!store.remove(permanent)) {
            return false;
        }
        byController.remove(permanent.controller(), permanent);
        return true;
    }

    @Override
    public Optional<Card> sourceCard(Permanent permanent) {
        return permanent.source() instanceof Card card ? Optional.of(card) : Optional.empty();
    }

    @Override
    public List<Permanent> controlledBy(Player controller) {
        return List.copyOf(byController.get(controller));
    }

    @Override
    public List<Permanent> ofType(Type type) {
        return stream().filter(p -> p.types().contains(type)).toList();
    }

    @Override
    public List<Permanent> controlledByOfType(Player controller, Type type) {
        return byController.get(controller).stream()
                .filter(p -> p.types().contains(type))
                .toList();
    }

    @Override
    public List<Permanent> all() {
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
    public boolean contains(Permanent object) {
        return store.contains(object, this);
    }

    @Override
    public boolean containsObject(GameObject object) {
        return store.contains(object, this);
    }

    @Override
    public Stream<Permanent> stream() {
        return store.stream(this, Permanent.class);
    }
}
