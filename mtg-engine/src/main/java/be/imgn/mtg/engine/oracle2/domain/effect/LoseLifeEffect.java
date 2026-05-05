package be.imgn.mtg.engine.oracle2.domain.effect;

import static java.util.Objects.requireNonNull;

import be.imgn.mtg.engine.oracle2.domain.Amount;
import be.imgn.mtg.engine.oracle2.domain.selector.Selector;

/// "X loses N life." ({@mtg.rule 119.3}).
public record LoseLifeEffect(Selector who, Amount amount) implements Effect {
    public LoseLifeEffect {
        requireNonNull(who);
        requireNonNull(amount);
    }
}
