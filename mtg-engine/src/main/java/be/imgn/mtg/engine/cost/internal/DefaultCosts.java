package be.imgn.mtg.engine.cost.internal;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collector;

import be.imgn.mtg.engine.characteristics.internal.AbstractCharacteristics;
import be.imgn.mtg.engine.cost.Cost;
import be.imgn.mtg.engine.cost.Costs;

/// Default implementation of Costs.
public final class DefaultCosts extends AbstractCharacteristics<Cost, Costs> implements Costs {

    private static final DefaultCosts EMPTY = new DefaultCosts(Set.of());

    private DefaultCosts(Set<Cost> elements) {
        super(elements);
    }

    public static Costs empty() {
        return EMPTY;
    }

    public static Costs of(Cost... costs) {
        if (costs.length == 0) {
            return EMPTY;
        }
        var set = new LinkedHashSet<>(Arrays.asList(costs));
        return new DefaultCosts(Collections.unmodifiableSet(set));
    }

    public static Costs.Builder builder() {
        return new Builder();
    }

    /// Returns a Collector for Costs.
    public static Collector<Cost, ?, Costs> collector() {
        return Collector.of(
                () -> new LinkedHashSet<Cost>(),
                LinkedHashSet::add,
                (s1, s2) -> {
                    s1.addAll(s2);
                    return s1;
                },
                set -> set.isEmpty() ? EMPTY : new DefaultCosts(Collections.unmodifiableSet(new LinkedHashSet<>(set))));
    }

    @Override
    public Costs.Builder toBuilder() {
        return new Builder(elements);
    }

    static final class Builder extends AbstractCharacteristics.Builder<Cost, Costs, Costs.Builder>
            implements Costs.Builder {

        Builder() {
            super(
                    new LinkedHashSet<>(),
                    () -> EMPTY,
                    set -> new DefaultCosts(Collections.unmodifiableSet(new LinkedHashSet<>(set))));
        }

        Builder(Set<Cost> initial) {
            super(
                    new LinkedHashSet<>(initial),
                    () -> EMPTY,
                    set -> new DefaultCosts(Collections.unmodifiableSet(new LinkedHashSet<>(set))));
        }
    }
}
