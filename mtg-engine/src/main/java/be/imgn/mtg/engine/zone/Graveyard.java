package be.imgn.mtg.engine.zone;

import java.util.List;
import java.util.Optional;

import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.object.Card;

/// The graveyard zone - a player's discard pile ({@mtg.rule 404}).
///
/// The graveyard is an ordered zone where destroyed creatures, discarded cards,
/// countered spells, and used instants and sorceries are placed. The graveyard
/// is public - all players can see its contents.
///
/// Graveyards are per-player zones: each player has their own graveyard.
/// Cards are typically added to the top and the order matters for some effects.
///
/// @see ZoneType#GRAVEYARD
public non-sealed interface Graveyard extends Zone<Card> {

    /// Returns the player who owns this graveyard.
    ///
    /// @return the owner
    Player owner();

    /// Returns the top card of the graveyard without removing it.
    ///
    /// @return the top card, or empty if the graveyard is empty
    Optional<Card> peekTop();

    /// Puts a card on top of the graveyard.
    ///
    /// This is the normal way cards enter the graveyard.
    ///
    /// @param card the card to put
    void put(Card card);

    /// Removes a card from the graveyard.
    ///
    /// @param card the card to remove
    /// @return true if the card was found and removed
    boolean remove(Card card);

    /// Returns all cards in this graveyard in order (top to bottom).
    ///
    /// @return the cards in graveyard order
    List<Card> cards();

    @Override
    default ZoneType type() {
        return ZoneType.GRAVEYARD;
    }
}
