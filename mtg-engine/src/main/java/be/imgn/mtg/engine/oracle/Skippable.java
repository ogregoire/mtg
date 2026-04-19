package be.imgn.mtg.engine.oracle;

import be.imgn.mtg.engine.turn.Phase;
import be.imgn.mtg.engine.turn.Step;

/// What a `skip` effect replaces (rule 614.10). Skip targets are:
/// - an entire turn ("skip your next turn"),
/// - a phase ("skip your combat phase"),
/// - or a step ("skip your upkeep step").
public sealed interface Skippable {

    /// Skip the whole turn.
    enum Turn implements Skippable {
        TURN
    }

    /// Skip a named phase (main, combat, beginning, ending).
    record OfPhase(Phase phase) implements Skippable {}

    /// Skip a named step (upkeep, draw, untap, end, combat damage, …).
    record OfStep(Step step) implements Skippable {}
}
