package be.imgn.mtg.engine.zone.internal;

import java.util.List;
import java.util.stream.Stream;

import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.object.Card;
import be.imgn.mtg.engine.object.GameObject;
import be.imgn.mtg.engine.state.internal.ObjectStore;
import be.imgn.mtg.engine.zone.Hand;

/// Default implementation of [Hand].
///
/// Backed by the central [ObjectStore]. Hand order is not meaningful.
public final class DefaultHand implements Hand {

    private final ObjectStore store;
    private final Player owner;

    /// Creates a new hand for the given player, backed by the given store.
    ///
    /// @param store the central object store
    /// @param owner the player who owns this hand
    public DefaultHand(ObjectStore store, Player owner) {
        this.store = store;
        this.owner = owner;
    }

    @Override
    public Player owner() {
        return owner;
    }

    @Override
    public void add(Card card) {
        store.add(card, this);
    }

    @Override
    public void addAll(List<Card> cards) {
        for (var card : cards) {
            store.add(card, this);
        }
    }

    @Override
    public boolean remove(Card card) {
        return store.remove(card);
    }

    @Override
    public List<Card> cards() {
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
    public boolean contains(Card object) {
        return store.contains(object, this);
    }

    @Override
    public boolean containsObject(GameObject object) {
        return store.contains(object, this);
    }

    @Override
    public Stream<Card> stream() {
        return store.stream(this, Card.class);
    }
}
