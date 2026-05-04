package be.imgn.mtg.engine.oracle2.domain.selector;

import static java.util.Objects.requireNonNull;

import be.imgn.mtg.engine.oracle2.domain.AmountMatcher;
import be.imgn.mtg.engine.oracle2.domain.CounterType;

/// Selects an object by counters on it ({@mtg.rule 122}). Object-side
/// counterpart to [PlayerCounterSelector].
///
/// Examples:
/// - "creature with a +1/+1 counter on it" →
///   `HasCounters(PtCounter(1,1), AtLeast(Exact(1)))`.
/// - "creature with three or more +1/+1 counters" →
///   `HasCounters(PtCounter(1,1), AtLeast(Exact(3)))`.
/// - "creature with no counters on it" →
///   `HasCounters(Any.ANY, Exactly(Exact(0)))`.
public sealed interface ObjectCounterSelector extends ObjectPropertySelector permits ObjectCounterSelector.HasCounters {

    /// Predicate that the object carries a count of counters of the
    /// given type that satisfies the matcher. `type` may be
    /// [CounterType.Any#ANY] for type-agnostic counts ("with no
    /// counters", "with a counter").
    record HasCounters(CounterType type, AmountMatcher count) implements ObjectCounterSelector {
        public HasCounters {
            requireNonNull(type);
            requireNonNull(count);
        }
    }
}
