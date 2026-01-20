package be.imgn.mtg.engine.turn;

import be.imgn.mtg.engine.event.Event;

/// Event fired when a step ends ({@mtg.rule 500}).
///
/// @param step the type of step that ended
/// @param occurrence the occurrence number of this step in the current turn (1-indexed)
public record StepEndedEvent(StepType step, int occurrence) implements Event {}
