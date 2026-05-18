package be.imgn.mtg.engine.oracle2.domain.effect;

import static java.util.Objects.requireNonNull;

import be.imgn.mtg.engine.oracle2.domain.Amount;
import be.imgn.mtg.engine.oracle2.domain.selector.Selector;

/// "X scries N." ({@mtg.rule 701.22}).
public record ScryEffect(Selector who, Amount amount) implements Effect {
    public ScryEffect {
        requireNonNull(who);
        requireNonNull(amount);
    }
}
