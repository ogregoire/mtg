package be.imgn.mtg.engine.zone.internal;

import java.util.List;
import java.util.stream.Stream;

import be.imgn.mtg.engine.object.Card;
import be.imgn.mtg.engine.object.GameObject;
import be.imgn.mtg.engine.state.internal.ObjectStore;
import be.imgn.mtg.engine.zone.Exile;

/// Default implementation of [Exile].
///
/// Backed by the central [ObjectStore]. Face-down status is tracked by the store.
public final class DefaultExile implements Exile {

    private final ObjectStore store;

    /// Creates a new empty exile zone backed by the given store.
    ///
    /// @param store the central object store
    public DefaultExile(ObjectStore store) {
        this.store = store;
    }

    @Override
    public void exile(Card card) {
        store.add(card, this);
    }

    @Override
    public void exileFaceDown(Card card) {
        store.add(card, this);
        store.markFaceDown(card);
    }

    @Override
    public boolean isFaceDown(Card card) {
        return store.isFaceDown(card);
    }

    @Override
    public boolean remove(Card card) {
        return store.remove(card);
    }

    @Override
    public List<Card> faceUp() {
        return stream().filter(card -> !store.isFaceDown(card)).toList();
    }

    @Override
    public List<Card> all() {
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
