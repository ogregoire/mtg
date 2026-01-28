package be.imgn.mtg.engine.turn;

import be.imgn.mtg.engine.event.Event;

/// Event fired when a step ends ({@mtg.rule 500.1}).
///
/// @param step the step that ended
/// @param occurrence the occurrence number within the current phase
public record StepEndedEvent(Step step, int occurrence) implements Event {}
