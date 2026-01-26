package be.imgn.mtg.engine.turn2;

import be.imgn.mtg.engine.event.Event;
import be.imgn.mtg.engine.game.Player;

/// Event fired when a turn begins ({@mtg.rule 500}).
///
/// @param turnNumber the turn number (starting at 1)
/// @param activePlayer the player whose turn is starting
public record TurnStartedEvent(int turnNumber, Player activePlayer) implements Event {}
