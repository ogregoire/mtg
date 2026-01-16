package be.imgn.mtg.engine.characteristics.internal;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import be.imgn.mtg.engine.characteristics.Cost;
import be.imgn.mtg.engine.characteristics.Costs;

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
