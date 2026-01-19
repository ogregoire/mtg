package be.imgn.mtg.engine.zone;

import java.util.List;
import java.util.Optional;

import be.imgn.mtg.engine.object.Card;
import be.imgn.mtg.engine.object.ObjectId;

/// The exile zone - where exiled cards go ({@mtg.rule 406}).
///
/// Exile is a shared zone where cards are placed when exiled by game effects.
/// Exiled cards are normally face-up and visible to all players, but some effects
/// exile cards face-down.
///
/// Unlike the graveyard, exiled cards are generally harder to interact with
/// unless a specific effect allows it.
///
/// @see ZoneType#EXILE
public non-sealed interface Exile extends Zone<Card> {

    /// Exiles a card face-up.
    ///
    /// @param card the card to exile
    void exile(Card card);

    /// Exiles a card face-down.
    ///
    /// Face-down exiled cards are not visible to any player.
    ///
    /// @param card the card to exile face-down
    void exileFaceDown(Card card);

    /// Returns true if the given card is exiled face-down.
    ///
    /// @param id the card's ID
    /// @return true if face-down, false if face-up or not found
    boolean isFaceDown(ObjectId id);

    /// Removes a card from exile.
    ///
    /// @param id the ID of the card to remove
    /// @return the removed card, or empty if not found
    Optional<Card> remove(ObjectId id);

    /// Returns all face-up exiled cards.
    ///
    /// @return face-up exiled cards
    List<Card> faceUp();

    /// Returns all exiled cards (both face-up and face-down).
    ///
    /// Note: Face-down cards are normally hidden, but this method returns all for game logic.
    ///
    /// @return all exiled cards
    List<Card> all();

    @Override
    default ZoneType type() {
        return ZoneType.EXILE;
    }
}
