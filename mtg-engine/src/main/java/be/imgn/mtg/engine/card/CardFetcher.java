package be.imgn.mtg.engine.card;

import java.util.Optional;

import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.object.Card;

/// Fetches cards from the card database by name.
///
/// Looks up card data, parses types, mana costs, and abilities,
/// and returns a fully constructed [Card] object.
public interface CardFetcher {

    /// Fetches a card by exact name.
    ///
    /// @param name the card name (case-insensitive)
    /// @param owner the player who owns the card
    /// @return the card, or empty if not found
    Optional<Card> fetchByName(String name, Player owner);
}
