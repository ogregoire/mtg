package be.imgn.mtg.engine.mana;

import be.imgn.mtg.engine.event.Event;
import be.imgn.mtg.engine.game.Player;

/// Event fired when mana is added to a player's mana pool ({@mtg.rule 106}).
///
/// @param player the player who received the mana
/// @param mana the mana that was added
public record ManaAddedEvent(Player player, Mana mana) implements Event {}
