package be.imgn.mtg.engine.turn;

import be.imgn.mtg.engine.event.Event;

/// Event fired when a step begins ({@mtg.rule 500.1}).
///
/// @param step the step that is starting
/// @param occurrence the occurrence number within the current phase (usually 1)
public record StepStartedEvent(Step step, int occurrence) implements Event {}
