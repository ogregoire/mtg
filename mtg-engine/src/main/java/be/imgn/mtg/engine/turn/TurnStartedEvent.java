package be.imgn.mtg.engine.turn;

import be.imgn.mtg.engine.event.Event;

/// Event fired when a new turn begins.
public record TurnStartedEvent(int turnNumber) implements Event {}
