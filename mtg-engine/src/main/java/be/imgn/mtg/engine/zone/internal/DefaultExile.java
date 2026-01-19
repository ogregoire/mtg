package be.imgn.mtg.engine.zone.internal;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import be.imgn.mtg.engine.object.Card;
import be.imgn.mtg.engine.object.ObjectId;
import be.imgn.mtg.engine.zone.Exile;

/// Default implementation of [Exile].
///
/// Tracks face-down status separately from the card storage.
public final class DefaultExile extends AbstractZone<Card> implements Exile {

    /// Set of IDs of face-down cards.
    private final Set<ObjectId> faceDownIds = new HashSet<>();

    /// Creates a new empty exile zone.
    public DefaultExile() {}

    @Override
    public void exile(Card card) {
        index(card);
    }

    @Override
    public void exileFaceDown(Card card) {
        index(card);
        faceDownIds.add(card.id());
    }

    @Override
    public boolean isFaceDown(ObjectId id) {
        return faceDownIds.contains(id);
    }

    @Override
    public Optional<Card> remove(ObjectId id) {
        faceDownIds.remove(id);
        return Optional.ofNullable(unindex(id));
    }

    @Override
    public List<Card> faceUp() {
        return objectsById.values().stream()
                .filter(card -> !faceDownIds.contains(card.id()))
                .toList();
    }

    @Override
    public List<Card> all() {
        return List.copyOf(objectsById.values());
    }

    @Override
    public Stream<Card> stream() {
        return objectsById.values().stream();
    }
}
