package be.imgn.mtg.engine.zone;

import java.util.List;
import java.util.Optional;

import be.imgn.mtg.engine.characteristics.Type;
import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.object.Card;
import be.imgn.mtg.engine.object.Permanent;
import be.imgn.mtg.engine.object.Token;

/// The battlefield zone - where permanents exist ({@mtg.rule 403}).
///
/// The battlefield is a shared zone where all permanents exist. Permanents enter
/// the battlefield from spells resolving or from effects that put cards directly
/// onto the battlefield.
///
/// The battlefield does not hold cards directly - it holds [Permanent] objects.
/// When a card enters the battlefield, a new Permanent is created with the card
/// as its source. The same applies to tokens.
///
/// The battlefield is shared by all players but each permanent has a controller.
///
/// @see Permanent
/// @see ZoneType#BATTLEFIELD
public non-sealed interface Battlefield extends Zone<Permanent> {

    /// Enters a card onto the battlefield, creating a new permanent.
    ///
    /// @param card the card entering the battlefield
    /// @param controller the player who will control the permanent
    /// @return the created permanent
    Permanent enter(Card card, Player controller);

    /// Enters a token onto the battlefield, creating a new permanent.
    ///
    /// @param token the token entering the battlefield
    /// @param controller the player who will control the permanent
    /// @return the created permanent
    Permanent enter(Token token, Player controller);

    /// Enters an already-constructed permanent onto the battlefield.
    ///
    /// Used when a permanent needs to be created with specific characteristics.
    ///
    /// @param permanent the permanent to add
    void enter(Permanent permanent);

    /// Removes a permanent from the battlefield.
    ///
    /// When a permanent leaves the battlefield, it is destroyed. The source (Card/Token)
    /// should be moved to the appropriate destination zone.
    ///
    /// @param permanent the permanent to remove
    /// @return true if the permanent was found and removed
    boolean remove(Permanent permanent);

    /// Returns the source card of a permanent that was removed.
    ///
    /// This is a convenience method for zone change handling. When a card-backed permanent
    /// leaves the battlefield, this retrieves the original card to move to the destination.
    ///
    /// @param permanent the permanent that left the battlefield
    /// @return the source card, or empty if the permanent was token-based
    default Optional<Card> sourceCard(Permanent permanent) {
        return permanent.source() instanceof Card card ? Optional.of(card) : Optional.empty();
    }

    /// Returns all permanents controlled by the given player.
    ///
    /// @param controller the player
    /// @return the permanents controlled by that player
    List<Permanent> controlledBy(Player controller);

    /// Returns all permanents of the given type.
    ///
    /// @param type the card type
    /// @return the permanents with that type
    List<Permanent> ofType(Type type);

    /// Returns all permanents controlled by the given player of the given type.
    ///
    /// @param controller the player
    /// @param type the card type
    /// @return the matching permanents
    List<Permanent> controlledByOfType(Player controller, Type type);

    /// Returns all permanents on the battlefield.
    ///
    /// @return all permanents
    List<Permanent> all();

    @Override
    default ZoneType type() {
        return ZoneType.BATTLEFIELD;
    }
}
