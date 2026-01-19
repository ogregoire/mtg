package be.imgn.mtg.engine.zone;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.object.Card;
import be.imgn.mtg.engine.object.ObjectId;

/// The library zone - a player's deck ({@mtg.rule 401}).
///
/// The library is an ordered zone where a player's deck exists during the game.
/// Cards are drawn from the top of the library. The library is hidden - players
/// cannot normally see the order or contents of libraries.
///
/// Libraries are per-player zones: each player has their own library.
///
/// @see ZoneType#LIBRARY
public non-sealed interface Library extends Zone<Card> {

    /// Returns the player who owns this library.
    ///
    /// @return the owner
    Player owner();

    /// Returns the top card of the library without removing it.
    ///
    /// @return the top card, or empty if the library is empty
    Optional<Card> peekTop();

    /// Returns the top N cards of the library without removing them.
    ///
    /// @param count the number of cards to peek
    /// @return the top cards (may be fewer than requested if library has fewer cards)
    List<Card> peekTop(int count);

    /// Draws (removes) the top card from the library.
    ///
    /// @return the drawn card, or empty if the library is empty
    Optional<Card> drawTop();

    /// Draws (removes) the top N cards from the library.
    ///
    /// @param count the number of cards to draw
    /// @return the drawn cards (may be fewer than requested)
    List<Card> drawTop(int count);

    /// Returns the bottom card of the library without removing it.
    ///
    /// @return the bottom card, or empty if the library is empty
    Optional<Card> peekBottom();

    /// Puts a card on top of the library.
    ///
    /// @param card the card to put on top
    void putOnTop(Card card);

    /// Puts multiple cards on top of the library in the given order.
    ///
    /// The first card in the list will be the topmost card.
    ///
    /// @param cards the cards to put on top
    void putOnTop(List<Card> cards);

    /// Puts a card on the bottom of the library.
    ///
    /// @param card the card to put on bottom
    void putOnBottom(Card card);

    /// Puts multiple cards on the bottom of the library in the given order.
    ///
    /// The first card in the list will be closest to the rest of the library.
    ///
    /// @param cards the cards to put on bottom
    void putOnBottom(List<Card> cards);

    /// Removes a specific card from the library.
    ///
    /// @param id the ID of the card to remove
    /// @return the removed card, or empty if not found
    Optional<Card> remove(ObjectId id);

    /// Shuffles the library.
    ///
    /// After shuffling, the order of cards is randomized.
    void shuffle();

    /// Searches for a card matching a predicate.
    ///
    /// Note: Searching the library typically requires a game effect that allows it.
    ///
    /// @param predicate the condition to match
    /// @return the matching cards
    List<Card> search(Predicate<Card> predicate);

    @Override
    default ZoneType type() {
        return ZoneType.LIBRARY;
    }
}
