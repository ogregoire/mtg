package be.imgn.mtg.engine.turn;

import be.imgn.mtg.engine.event.Event;

/// Event fired when a phase begins ({@mtg.rule 500}).
///
/// @param phase the type of phase that started
/// @param occurrence the occurrence number of this phase in the current turn (1-indexed).
///                   For example, the pre-combat main phase is occurrence 1,
///                   and the post-combat main phase is occurrence 2.
public record PhaseStartedEvent(PhaseType phase, int occurrence) implements Event {}
