package be.imgn.mtg.engine.oracle2.domain.effect;

import static java.util.Objects.requireNonNull;

import be.imgn.mtg.engine.oracle2.domain.selector.Selector;

/// "Transform X." ({@mtg.rule 701.28}) — flip a double-faced
/// permanent to its other face. Kessig Prowler: "Transform this
/// creature." `selector` typically targets the activating permanent
/// itself via [be.imgn.mtg.engine.oracle2.domain.selector.SelfSelector].
public record TransformEffect(Selector selector) implements Effect {
    public TransformEffect {
        requireNonNull(selector);
    }
}
