package be.imgn.mtg.engine.turn;

import be.imgn.mtg.engine.event.Event;

/// Event fired when a phase ends ({@mtg.rule 500}).
///
/// @param phase the type of phase that ended
/// @param occurrence the occurrence number of this phase in the current turn (1-indexed)
public record PhaseEndedEvent(PhaseType phase, int occurrence) implements Event {}
