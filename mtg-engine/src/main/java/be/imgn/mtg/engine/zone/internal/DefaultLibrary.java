package be.imgn.mtg.engine.zone.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.object.Card;
import be.imgn.mtg.engine.object.GameObject;
import be.imgn.mtg.engine.state.internal.ObjectStore;
import be.imgn.mtg.engine.zone.Library;

/// Default implementation of [Library].
///
/// Backed by the central [ObjectStore]. Maintains a secondary ordered list
/// (index 0 = top of library) for deck ordering.
public final class DefaultLibrary implements Library {

    private final ObjectStore store;
    private final Player owner;

    /// Ordered list of cards (index 0 = top of library).
    private final List<Card> cards = new ArrayList<>();

    /// Creates a new library for the given player, backed by the given store.
    ///
    /// @param store the central object store
    /// @param owner the player who owns this library
    public DefaultLibrary(ObjectStore store, Player owner) {
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
    public List<Card> peekTop(int count) {
        if (count <= 0) {
            return List.of();
        }
        var actual = Math.min(count, cards.size());
        return List.copyOf(cards.subList(0, actual));
    }

    @Override
    public Optional<Card> drawTop() {
        if (cards.isEmpty()) {
            return Optional.empty();
        }
        var card = cards.removeFirst();
        store.remove(card);
        return Optional.of(card);
    }

    @Override
    public List<Card> drawTop(int count) {
        if (count <= 0 || cards.isEmpty()) {
            return List.of();
        }
        var actual = Math.min(count, cards.size());
        var drawn = new ArrayList<Card>(actual);
        for (var i = 0; i < actual; i++) {
            var card = cards.removeFirst();
            store.remove(card);
            drawn.add(card);
        }
        return List.copyOf(drawn);
    }

    @Override
    public Optional<Card> peekBottom() {
        return cards.isEmpty() ? Optional.empty() : Optional.of(cards.getLast());
    }

    @Override
    public void putOnTop(Card card) {
        cards.addFirst(card);
        store.add(card, this);
    }

    @Override
    public void putOnTop(List<Card> cardsToAdd) {
        // Add in reverse order so first card in list ends up on top
        for (var i = cardsToAdd.size() - 1; i >= 0; i--) {
            putOnTop(cardsToAdd.get(i));
        }
    }

    @Override
    public void putOnBottom(Card card) {
        cards.addLast(card);
        store.add(card, this);
    }

    @Override
    public void putOnBottom(List<Card> cardsToAdd) {
        for (var card : cardsToAdd) {
            putOnBottom(card);
        }
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
    public void shuffle() {
        Collections.shuffle(cards);
    }

    @Override
    public List<Card> search(Predicate<Card> predicate) {
        return cards.stream().filter(predicate).toList();
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
