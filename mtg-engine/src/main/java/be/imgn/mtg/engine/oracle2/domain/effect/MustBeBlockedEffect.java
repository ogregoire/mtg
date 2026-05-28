package be.imgn.mtg.engine.oracle2.domain.effect;

import static java.util.Objects.requireNonNull;

import be.imgn.mtg.engine.oracle2.domain.Duration;
import be.imgn.mtg.engine.oracle2.domain.selector.Selector;

/// "(subject) must be blocked if able (duration)." ({@mtg.rule
/// 509.1c}) — obligatory combat restriction: while `subject` is
/// attacking, the defender must declare at least one blocker for it
/// if any legal blocker is available. Gaea's Protector ("This
/// creature must be blocked if able.") uses the default
/// [Duration.Fixed#PERMANENT]. Sibling to the [CantBlockEffect] /
/// [CantBeBlockedEffect] family on the obligation side.
public record MustBeBlockedEffect(Selector subject, Duration duration) implements Effect {
    public MustBeBlockedEffect {
        requireNonNull(subject);
        requireNonNull(duration);
    }

    public MustBeBlockedEffect(Selector subject) {
        this(subject, Duration.Fixed.PERMANENT);
    }
}
