package be.imgn.mtg.engine.turn;

import be.imgn.mtg.engine.event.Event;

/// Event fired when a step begins ({@mtg.rule 500}).
///
/// @param step the type of step that started
/// @param occurrence the occurrence number of this step in the current turn (1-indexed).
///                   For most steps this is 1, but combat damage steps may occur
///                   multiple times due to first strike/double strike.
public record StepStartedEvent(StepType step, int occurrence) implements Event {}
