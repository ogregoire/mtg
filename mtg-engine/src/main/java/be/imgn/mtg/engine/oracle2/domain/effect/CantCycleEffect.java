package be.imgn.mtg.engine.oracle2.domain.effect;

import static java.util.Objects.requireNonNull;

import be.imgn.mtg.engine.oracle2.domain.Duration;
import be.imgn.mtg.engine.oracle2.domain.selector.Selector;

/// "[subject] can't cycle cards [duration]." Bars `subject` from
/// activating any cycling ability ({@mtg.rule 702.29}). Stabilizer
/// ("Players can't cycle cards.") uses the default
/// [Duration.Fixed#PERMANENT].
public record CantCycleEffect(Selector subject, Duration duration) implements Effect {
    public CantCycleEffect {
        requireNonNull(subject);
        requireNonNull(duration);
    }

    public CantCycleEffect(Selector subject) {
        this(subject, Duration.Fixed.PERMANENT);
    }
}
