package be.imgn.mtg.engine.turn;

import be.imgn.mtg.engine.event.Event;
import be.imgn.mtg.engine.game.Player;

/// Event fired when a new turn begins ({@mtg.rule 500}).
///
/// @param turnNumber the number of the turn that started (1-indexed)
/// @param activePlayer the player whose turn it is
public record TurnStartedEvent(int turnNumber, Player activePlayer) implements Event {}
