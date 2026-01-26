package be.imgn.mtg.engine.turn2;

import be.imgn.mtg.engine.event.Event;

/// Event fired when a phase ends ({@mtg.rule 500.1}).
///
/// @param phase the phase that ended
/// @param occurrence the occurrence number within this turn
public record PhaseEndedEvent(Phase phase, int occurrence) implements Event {}
