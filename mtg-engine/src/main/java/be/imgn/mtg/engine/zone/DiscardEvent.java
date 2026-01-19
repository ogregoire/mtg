package be.imgn.mtg.engine.zone;

import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.object.Card;

/// Event representing a card being discarded from hand ({@mtg.rule 701.8}).
///
/// Discarding moves a card from a player's hand to their graveyard. This is different
/// from other ways cards can go to the graveyard (mill, dies, etc.).
///
/// Discard events trigger abilities like madness and can be replaced.
///
/// @param card the card being discarded
/// @param player the player discarding
public record DiscardEvent(Card card, Player player) implements ZoneChangeEvent {

    @Override
    public ZoneType from() {
        return ZoneType.HAND;
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
