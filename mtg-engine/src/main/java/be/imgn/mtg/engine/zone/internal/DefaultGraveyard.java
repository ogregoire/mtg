package be.imgn.mtg.engine.zone.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.object.Card;
import be.imgn.mtg.engine.object.GameObject;
import be.imgn.mtg.engine.state.internal.ObjectStore;
import be.imgn.mtg.engine.zone.Graveyard;

/// Default implementation of [Graveyard].
///
/// Backed by the central [ObjectStore]. Maintains a secondary ordered list
/// (index 0 = top/most recently added) for graveyard ordering.
public final class DefaultGraveyard implements Graveyard {

    private final ObjectStore store;
    private final Player owner;

    /// Ordered list of cards (index 0 = top/most recently added).
    private final List<Card> cards = new ArrayList<>();

    /// Creates a new graveyard for the given player, backed by the given store.
    ///
    /// @param store the central object store
    /// @param owner the player who owns this graveyard
    public DefaultGraveyard(ObjectStore store, Player owner) {
        this.store = store;
        this.owner = owner;
    }

    @Override
    public Player owner() {
        return owner;
    }

    @Override
    public Optional<Card> peekTop() {
        return cards.isEmpty() ? Optional.empty() : Optional.of(cards.getFirst());
    }

    @Override
    public void put(Card card) {
        cards.addFirst(card);
        store.add(card, this);
    }

    @Override
    public boolean remove(Card card) {
        if (cards.remove(card)) {
            store.remove(card);
            return true;
        }
        return false;
    }

    @Override
    public List<Card> cards() {
        return List.copyOf(cards);
    }

    @Override
    public int size() {
        return cards.size();
    }

    @Override
    public boolean isEmpty() {
        return cards.isEmpty();
    }

    @Override
    public boolean contains(Card object) {
        return cards.contains(object);
    }

    @Override
    public boolean containsObject(GameObject object) {
        return cards.contains(object);
    }

    @Override
    public Stream<Card> stream() {
        return cards.stream();
    }
}
