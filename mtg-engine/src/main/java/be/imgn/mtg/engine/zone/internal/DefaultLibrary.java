package be.imgn.mtg.engine.zone.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.object.Card;
import be.imgn.mtg.engine.zone.Library;

/// Default implementation of [Library].
///
/// Uses a list to maintain card order with index 0 being the top of the library.
public final class DefaultLibrary extends AbstractZone<Card> implements Library {

    private final Player owner;

    /// Ordered list of cards (index 0 = top of library).
    private final List<Card> cards = new ArrayList<>();

    /// Creates a new library for the given player.
    ///
    /// @param owner the player who owns this library
    public DefaultLibrary(Player owner) {
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
        unindex(card);
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
            unindex(card);
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
        index(card);
        cards.addFirst(card);
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
        index(card);
        cards.addLast(card);
    }

    @Override
    public void putOnBottom(List<Card> cardsToAdd) {
        for (var card : cardsToAdd) {
            putOnBottom(card);
        }
    }

    @Override
    public boolean remove(Card card) {
        if (unindex(card)) {
            cards.remove(card);
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
    public Stream<Card> stream() {
        return cards.stream();
    }
}
