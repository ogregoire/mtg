package be.imgn.mtg.engine.turn;

import be.imgn.mtg.engine.event.Event;

/// Event fired when a phase begins ({@mtg.rule 500.1}).
///
/// @param phase the phase that is starting
/// @param occurrence the occurrence number within this turn (1 for first, 2 for second main, etc.)
public record PhaseStartedEvent(Phase phase, int occurrence) implements Event {}
