package be.imgn.mtg.engine.oracle2.domain.effect;

import static java.util.Objects.requireNonNull;

import be.imgn.mtg.engine.oracle2.domain.Duration;
import be.imgn.mtg.engine.oracle2.domain.selector.Selector;

/// "(subject) can't be countered (duration)." Counter-immunity
/// ({@mtg.rule 701.5}) — spells matching `subject` cannot be
/// countered by any counter effect. Gaea's Herald ("Creature spells
/// can't be countered.") uses the default [Duration.Fixed#PERMANENT].
public record CantBeCounteredEffect(Selector subject, Duration duration) implements Effect {
    public CantBeCounteredEffect {
        requireNonNull(subject);
        requireNonNull(duration);
    }

    public CantBeCounteredEffect(Selector subject) {
        this(subject, Duration.Fixed.PERMANENT);
    }
}
