package be.imgn.mtg.engine.oracle2.domain.effect;

import static java.util.Objects.requireNonNull;

import be.imgn.mtg.engine.oracle2.domain.Amount;
import be.imgn.mtg.engine.oracle2.domain.selector.Selector;

/// "(source) deal(s) (amount) damage to (target)." ({@mtg.rule 119})
/// — `source` deals `amount` damage to `target`. Shock ("~ deals 2
/// damage to any target."), Blaze ("~ deals X damage to any
/// target.").
public record DamageEffect(Selector source, Amount amount, Selector target) implements Effect {
    public DamageEffect {
        requireNonNull(source);
        requireNonNull(amount);
        requireNonNull(target);
    }
}
