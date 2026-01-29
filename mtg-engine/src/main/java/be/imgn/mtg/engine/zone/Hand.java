package be.imgn.mtg.engine.zone;

import java.util.List;

import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.object.Card;

/// The hand zone - cards held by a player ({@mtg.rule 402}).
///
/// The hand is where cards go when drawn and where they're played from.
/// The hand is hidden - only the owner can normally see its contents.
///
/// Hands are per-player zones: each player has their own hand.
///
/// @see ZoneType#HAND
public non-sealed interface Hand extends Zone<Card> {

    /// Returns the player who owns this hand.
    ///
    /// @return the owner
    Player owner();

    /// Adds a card to this hand.
    ///
    /// @param card the card to add
    void add(Card card);

    /// Adds multiple cards to this hand.
    ///
    /// @param cards the cards to add
    void addAll(List<Card> cards);

    /// Removes a card from this hand.
    ///
    /// @param card the card to remove
    /// @return true if the card was found and removed
    boolean remove(Card card);

    /// Returns all cards in this hand as a list.
    ///
    /// The order is not guaranteed to be meaningful.
    ///
    /// @return the cards in hand
    List<Card> cards();

    @Override
    default ZoneType type() {
        return ZoneType.HAND;
    }
}
