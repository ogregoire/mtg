package be.imgn.mtg.engine.oracle;

import be.imgn.mtg.engine.turn.Phase;
import be.imgn.mtg.engine.turn.Step;

/// What a `skip` effect replaces (rule 614.10). Skip targets are:
/// - an entire turn ("skip your next turn"),
/// - a phase ("skip your combat phase"),
/// - or a step ("skip your upkeep step").
public sealed interface Skippable {

    /// Skip one or more whole turns. `count` is the number of
    /// consecutive turns to skip (Eater of Days: "skip your next two
    /// turns." ⇒ `count = 2`); the common singular form defaults to 1.
    record Turn(Amount count) implements Skippable {
        public static Turn one() {
            return new Turn(Amount.exact(1));
        }
    }

    /// Skip a named phase (main, combat, beginning, ending).
    record OfPhase(Phase phase) implements Skippable {}

    /// Skip a named step (upkeep, draw, untap, end, combat damage, …).
    record OfStep(Step step) implements Skippable {}
}
