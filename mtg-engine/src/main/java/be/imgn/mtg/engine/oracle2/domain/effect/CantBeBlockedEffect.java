package be.imgn.mtg.engine.oracle2.domain.effect;

import static java.util.Objects.requireNonNull;

import be.imgn.mtg.engine.oracle2.domain.Duration;
import be.imgn.mtg.engine.oracle2.domain.selector.Selector;

/// "[subject] can't be blocked [duration]." Passive combat
/// restriction ({@mtg.rule 509.1b}) — `subject` may not be declared
/// as a block target. Slither Blade et al. ("This creature can't be
/// blocked.") use the default [Duration.Fixed#PERMANENT]. Distinct
/// from [CantBlockEffect] because the rules-side modification flips
/// from "may not declare blocks" to "may not be a block target".
public record CantBeBlockedEffect(Selector subject, Duration duration) implements Effect {
    public CantBeBlockedEffect {
        requireNonNull(subject);
        requireNonNull(duration);
    }

    public CantBeBlockedEffect(Selector subject) {
        this(subject, Duration.Fixed.PERMANENT);
    }
}
