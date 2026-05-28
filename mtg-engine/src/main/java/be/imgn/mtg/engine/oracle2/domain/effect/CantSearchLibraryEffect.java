package be.imgn.mtg.engine.oracle2.domain.effect;

import static java.util.Objects.requireNonNull;

import be.imgn.mtg.engine.oracle2.domain.Duration;
import be.imgn.mtg.engine.oracle2.domain.selector.Selector;

/// "[subject] can't search libraries [duration]." Bars `subject`
/// from every search-a-library effect ({@mtg.rule 701.19}). Mindlock
/// Orb ("Players can't search libraries.") uses the default
/// [Duration.Fixed#PERMANENT].
public record CantSearchLibraryEffect(Selector subject, Duration duration) implements Effect {
    public CantSearchLibraryEffect {
        requireNonNull(subject);
        requireNonNull(duration);
    }

    public CantSearchLibraryEffect(Selector subject) {
        this(subject, Duration.Fixed.PERMANENT);
    }
}
