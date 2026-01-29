package be.imgn.mtg.engine.zone.internal;

import java.util.List;
import java.util.stream.Stream;

import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.object.Card;
import be.imgn.mtg.engine.zone.Hand;

/// Default implementation of [Hand].
///
/// Uses the inherited set for storage since hand order is not meaningful.
public final class DefaultHand extends AbstractZone<Card> implements Hand {

    private final Player owner;

    /// Creates a new hand for the given player.
    ///
    /// @param owner the player who owns this hand
    public DefaultHand(Player owner) {
        this.owner = owner;
    }

    @Override
    public Player owner() {
        return owner;
    }

    @Override
    public void add(Card card) {
        index(card);
    }

    @Override
    public void addAll(List<Card> cards) {
        for (var card : cards) {
            index(card);
        }
    }

    @Override
    public boolean remove(Card card) {
        return unindex(card);
    }

    @Override
    public List<Card> cards() {
        return List.copyOf(objects);
    }

    @Override
    public Stream<Card> stream() {
        return objects.stream();
    }
}
