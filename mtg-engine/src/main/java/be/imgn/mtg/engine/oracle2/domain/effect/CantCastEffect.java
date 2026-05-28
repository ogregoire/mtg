package be.imgn.mtg.engine.oracle2.domain.effect;

import static java.util.Objects.requireNonNull;

import be.imgn.mtg.engine.oracle2.domain.Duration;
import be.imgn.mtg.engine.oracle2.domain.selector.Selector;

/// "[subject] can't cast [spells] [duration]." Bars `subject` from
/// casting spells matching `spells` ({@mtg.rule 601}). Grid Monitor
/// / Steel Golem ("You can't cast creature spells.") use the default
/// [Duration.Fixed#PERMANENT] with a creature-spell stack selector.
public record CantCastEffect(Selector subject, Selector spells, Duration duration) implements Effect {
    public CantCastEffect {
        requireNonNull(subject);
        requireNonNull(spells);
        requireNonNull(duration);
    }

    public CantCastEffect(Selector subject, Selector spells) {
        this(subject, spells, Duration.Fixed.PERMANENT);
    }
}
