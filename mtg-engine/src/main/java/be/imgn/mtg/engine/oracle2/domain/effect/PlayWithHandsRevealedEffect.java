package be.imgn.mtg.engine.oracle2.domain.effect;

import static java.util.Objects.requireNonNull;

import be.imgn.mtg.engine.oracle2.domain.Duration;
import be.imgn.mtg.engine.oracle2.domain.selector.Selector;

/// "(who) play(s) with their hands revealed (duration)." Continuous
/// hidden-information modifier ({@mtg.rule 408}). Revelation:
/// "Players play with their hands revealed.". The hand-owner is
/// the subject (the parent [#who]); the "their" is the anaphoric
/// possessive — left out of the AST per the simplified hand model.
public record PlayWithHandsRevealedEffect(Selector who, Duration duration) implements Effect {
    public PlayWithHandsRevealedEffect {
        requireNonNull(who);
        requireNonNull(duration);
    }

    public PlayWithHandsRevealedEffect(Selector who) {
        this(who, Duration.Fixed.PERMANENT);
    }
}
