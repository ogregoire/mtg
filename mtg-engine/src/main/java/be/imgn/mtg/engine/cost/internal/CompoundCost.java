package be.imgn.mtg.engine.cost.internal;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import be.imgn.mtg.engine.cost.Cost;

/// A compound cost composed of multiple sub-costs ({@mtg.rule 118.8}).
///
/// @param costs the individual costs (at least 2)
public record CompoundCost(List<Cost> costs) implements Cost {
    /// Creates a new compound cost.
    public CompoundCost {
        Objects.requireNonNull(costs, "costs");
        if (costs.size() < 2) {
            throw new IllegalArgumentException("CompoundCost requires at least 2 costs, got " + costs.size());
        }
        costs = List.copyOf(costs);
    }

    @Override
    public String description() {
        return costs.stream().map(Cost::description).collect(Collectors.joining(", "));
    }

    @Override
    public boolean isLoyaltyCost() {
        return costs.stream().anyMatch(Cost::isLoyaltyCost);
    }
}
