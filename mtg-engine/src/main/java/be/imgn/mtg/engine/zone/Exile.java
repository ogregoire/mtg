package be.imgn.mtg.engine.zone;

import java.util.List;

import be.imgn.mtg.engine.object.Card;

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
    /// @param card the card to check
    /// @return true if face-down, false if face-up or not found
    boolean isFaceDown(Card card);

    /// Removes a card from exile.
    ///
    /// @param card the card to remove
    /// @return true if the card was found and removed
    boolean remove(Card card);

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
