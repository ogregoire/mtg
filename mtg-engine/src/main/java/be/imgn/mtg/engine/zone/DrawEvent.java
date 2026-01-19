package be.imgn.mtg.engine.zone;

import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.object.Card;

/// Event representing a card being drawn from a library ({@mtg.rule 121}).
///
/// Drawing is a special action that moves a card from the top of a player's library
/// to their hand. Cards are drawn one at a time ({@mtg.rule 121.2}), so even when
/// multiple cards are drawn, each is a separate draw event.
///
/// Draw events can be replaced (e.g., "If you would draw a card, instead...").
///
/// @param card the card being drawn
/// @param player the player drawing the card
public record DrawEvent(Card card, Player player) implements ZoneChangeEvent {

    @Override
    public ZoneType from() {
        return ZoneType.LIBRARY;
    }

    @Override
    public ZoneType to() {
        return ZoneType.HAND;
    }

    @Override
    public Player affectedPlayer() {
        return player;
    }
}
