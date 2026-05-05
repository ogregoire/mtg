package be.imgn.mtg.engine.oracle2.domain.effect;

import static java.util.Objects.requireNonNull;

import be.imgn.mtg.engine.oracle2.domain.Amount;
import be.imgn.mtg.engine.oracle2.domain.selector.Selector;

/// "X gains N life." ({@mtg.rule 119.3}).
public record GainLifeEffect(Selector who, Amount amount) implements Effect {
    public GainLifeEffect {
        requireNonNull(who);
        requireNonNull(amount);
    }
}
