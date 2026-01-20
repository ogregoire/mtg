package be.imgn.mtg.engine.turn;

import be.imgn.mtg.engine.event.Event;
import be.imgn.mtg.engine.game.Player;

/// Event fired when a turn ends ({@mtg.rule 500}).
///
/// @param turnNumber the number of the turn that ended (1-indexed)
/// @param activePlayer the player whose turn ended
public record TurnEndedEvent(int turnNumber, Player activePlayer) implements Event {}
