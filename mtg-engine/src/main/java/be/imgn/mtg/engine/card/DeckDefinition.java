package be.imgn.mtg.engine.card;

import java.util.HashSet;
import java.util.Set;

import be.imgn.mtg.engine.util.Multiset;

/// A deck definition representing the cards a player brings to a game.
///
/// Contains a main deck and a sideboard, each as an immutable [Multiset] of
/// [CardDefinition] instances. Pure data with no player reference. Format
/// legality checking is deferred to a later feature.
public final class DeckDefinition {

    private final Multiset<CardDefinition> cards;
    private final Multiset<CardDefinition> sideboard;

    private DeckDefinition(Multiset<CardDefinition> cards, Multiset<CardDefinition> sideboard) {
        this.cards = Multiset.copyOf(cards);
        this.sideboard = Multiset.copyOf(sideboard);
    }

    /// Returns the multiset of card definitions in the main deck.
    ///
    /// @return an immutable multiset of card definitions
    public Multiset<CardDefinition> cards() {
        return cards;
    }

    /// Returns the multiset of card definitions in the sideboard.
    ///
    /// @return an immutable multiset of card definitions
    public Multiset<CardDefinition> sideboard() {
        return sideboard;
    }

    /// Returns the total number of cards in the main deck (counting duplicates).
    ///
    /// @return the total card count
    public int size() {
        return cards.size();
    }

    /// Returns the total number of copies of the given card across main deck and sideboard.
    ///
    /// @param card the card definition to count
    /// @return the combined count
    public int countWithSideboard(CardDefinition card) {
        return cards.count(card) + sideboard.count(card);
    }

    /// Returns the number of copies of the given card in the main deck.
    ///
    /// @param card the card definition to count
    /// @return the number of copies
    public int count(CardDefinition card) {
        return cards.count(card);
    }

    /// Returns the set of unique card definitions across both main deck and sideboard.
    ///
    /// @return an unmodifiable set of distinct card definitions
    public Set<CardDefinition> uniqueCards() {
        var result = new HashSet<>(cards.elementSet());
        result.addAll(sideboard.elementSet());
        return Set.copyOf(result);
    }

    /// Creates a new builder for a deck definition.
    ///
    /// @return a new builder
    public static Builder builder() {
        return new Builder();
    }

    /// Builder for [DeckDefinition] instances.
    public static final class Builder {

        private final Multiset<CardDefinition> cards = Multiset.newHashMultiset();
        private final Multiset<CardDefinition> sideboard = Multiset.newHashMultiset();

        private Builder() {}

        /// Adds a single copy of a card to the main deck.
        ///
        /// @param card the card definition to add
        /// @return this builder
        public Builder add(CardDefinition card) {
            cards.add(card);
            return this;
        }

        /// Adds multiple copies of a card to the main deck.
        ///
        /// @param card  the card definition to add
        /// @param count the number of copies to add
        /// @return this builder
        public Builder add(CardDefinition card, int count) {
            cards.add(card, count);
            return this;
        }

        /// Adds a single copy of a card to the sideboard.
        ///
        /// @param card the card definition to add
        /// @return this builder
        public Builder addSideboard(CardDefinition card) {
            sideboard.add(card);
            return this;
        }

        /// Adds multiple copies of a card to the sideboard.
        ///
        /// @param card  the card definition to add
        /// @param count the number of copies to add
        /// @return this builder
        public Builder addSideboard(CardDefinition card, int count) {
            sideboard.add(card, count);
            return this;
        }

        /// Builds the deck definition.
        ///
        /// @return the deck definition
        public DeckDefinition build() {
            return new DeckDefinition(cards, sideboard);
        }
    }
}
