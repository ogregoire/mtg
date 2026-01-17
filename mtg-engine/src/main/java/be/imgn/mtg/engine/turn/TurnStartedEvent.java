package be.imgn.mtg.engine.turn;

import be.imgn.mtg.engine.event.Event;

/// Event fired when a new turn begins.
///
/// @param turnNumber the number of the turn that started
public record TurnStartedEvent(int turnNumber) implements Event {}
