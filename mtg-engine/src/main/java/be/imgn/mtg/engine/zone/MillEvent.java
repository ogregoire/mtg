package be.imgn.mtg.engine.zone;

import java.util.List;

import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.object.Card;

/// Event representing cards being milled ({@mtg.rule 701.13}).
///
/// Milling moves cards from the top of a player's library to their graveyard.
/// Unlike drawing, milling multiple cards is a single action with all cards
/// moving simultaneously.
///
/// @param cards the cards being milled
/// @param player the player being milled
public record MillEvent(List<Card> cards, Player player) implements ZoneChangeEvent {

    /// Creates a MillEvent with a defensive copy of the cards list.
    public MillEvent {
        cards = List.copyOf(cards);
    }

    @Override
    public ZoneType from() {
        return ZoneType.LIBRARY;
    }

    @Override
    public ZoneType to() {
        return ZoneType.GRAVEYARD;
    }

    @Override
    public Player affectedPlayer() {
        return player;
    }
}
