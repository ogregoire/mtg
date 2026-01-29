package be.imgn.mtg.engine.zone.internal;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import be.imgn.mtg.engine.object.Card;
import be.imgn.mtg.engine.zone.Exile;

/// Default implementation of [Exile].
///
/// Tracks face-down status separately from the card storage.
public final class DefaultExile extends AbstractZone<Card> implements Exile {

    /// Set of face-down cards.
    private final Set<Card> faceDown = new HashSet<>();

    /// Creates a new empty exile zone.
    public DefaultExile() {}

    @Override
    public void exile(Card card) {
        index(card);
    }

    @Override
    public void exileFaceDown(Card card) {
        index(card);
        faceDown.add(card);
    }

    @Override
    public boolean isFaceDown(Card card) {
        return faceDown.contains(card);
    }

    @Override
    public boolean remove(Card card) {
        faceDown.remove(card);
        return unindex(card);
    }

    @Override
    public List<Card> faceUp() {
        return objects.stream().filter(card -> !faceDown.contains(card)).toList();
    }

    @Override
    public List<Card> all() {
        return List.copyOf(objects);
    }

    @Override
    public Stream<Card> stream() {
        return objects.stream();
    }
}
