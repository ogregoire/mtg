package be.imgn.mtg.engine.action;

import be.imgn.mtg.engine.event.Event;
import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.object.Card;

/// Event recording the act of playing a land ({@mtg.rule 305.1}).
///
/// This is a simple notification event posted directly to the [EventBus][be.imgn.mtg.engine.event.EventBus],
/// not a [GameEvent][be.imgn.mtg.engine.event.GameEvent]. The [EventTracker][be.imgn.mtg.engine.event.EventTracker]
/// automatically records it for land drop counting.
///
/// @param player the player who played the land
/// @param land the land card that was played
public record LandPlayedEvent(Player player, Card land) implements Event {}
