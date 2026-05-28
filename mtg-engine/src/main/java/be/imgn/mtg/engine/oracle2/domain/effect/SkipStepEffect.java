package be.imgn.mtg.engine.oracle2.domain.effect;

import static java.util.Objects.requireNonNull;

import be.imgn.mtg.engine.oracle2.domain.Duration;
import be.imgn.mtg.engine.oracle2.domain.selector.Selector;
import be.imgn.mtg.engine.turn.Step;

/// "(who) skip(s) their (step) step(s) (duration)." Continuous step-
/// skip ({@mtg.rule 500.8}) — the matching step is omitted from
/// every turn within the duration's scope. Eon Hub ("Players skip
/// their upkeep steps.") uses [Duration.Fixed#PERMANENT] and
/// [Step#UPKEEP].
public record SkipStepEffect(Selector who, Step step, Duration duration) implements Effect {
    public SkipStepEffect {
        requireNonNull(who);
        requireNonNull(step);
        requireNonNull(duration);
    }

    public SkipStepEffect(Selector who, Step step) {
        this(who, step, Duration.Fixed.PERMANENT);
    }
}
