package be.imgn.mtg.engine.zone.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.object.Card;
import be.imgn.mtg.engine.object.ObjectId;
import be.imgn.mtg.engine.zone.Graveyard;

/// Default implementation of [Graveyard].
///
/// Uses a list to maintain card order with index 0 being the top of the graveyard.
public final class DefaultGraveyard extends AbstractZone<Card> implements Graveyard {

    private final Player owner;

    /// Ordered list of cards (index 0 = top/most recently added).
    private final List<Card> cards = new ArrayList<>();

    /// Creates a new graveyard for the given player.
    ///
    /// @param owner the player who owns this graveyard
    public DefaultGraveyard(Player owner) {
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
        index(card);
        cards.addFirst(card);
    }

    @Override
    public Optional<Card> remove(ObjectId id) {
        var card = unindex(id);
        if (card != null) {
            cards.remove(card);
        }
        return Optional.ofNullable(card);
    }

    @Override
    public List<Card> cards() {
        return List.copyOf(cards);
    }

    @Override
    public Stream<Card> stream() {
        return cards.stream();
    }
}
