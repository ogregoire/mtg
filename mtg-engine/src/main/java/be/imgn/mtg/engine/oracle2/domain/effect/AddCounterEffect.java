package be.imgn.mtg.engine.oracle2.domain.effect;

import static java.util.Objects.requireNonNull;

import be.imgn.mtg.engine.oracle2.domain.Amount;
import be.imgn.mtg.engine.oracle2.domain.CounterType;
import be.imgn.mtg.engine.oracle2.domain.selector.Selector;

/// "Put (count) (type) counter(s) on (target)." ({@mtg.rule 122.2})
/// — verb-first counter-placement effect. Battlegrowth ("Put a +1/+1
/// counter on target creature."), Scar ("Put a -1/-1 counter on
/// target creature."). The resolving player is the implicit actor;
/// only the count, counter type, and target are tracked.
public record AddCounterEffect(Amount count, CounterType counter, Selector target) implements Effect {
    public AddCounterEffect {
        requireNonNull(count);
        requireNonNull(counter);
        requireNonNull(target);
    }
}
