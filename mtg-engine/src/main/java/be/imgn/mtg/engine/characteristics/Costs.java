package be.imgn.mtg.engine.characteristics;

import be.imgn.mtg.engine.characteristics.internal.Characteristics;
import be.imgn.mtg.engine.characteristics.internal.DefaultCosts;

/// A collection of costs.
public interface Costs extends Characteristics<Cost> {

    /// Returns an empty Costs collection.
    static Costs empty() {
        return DefaultCosts.empty();
    }

    /// Returns a Costs collection containing the specified costs.
    static Costs of(Cost... costs) {
        return DefaultCosts.of(costs);
    }

    /// Returns a new builder for Costs.
    static Builder builder() {
        return DefaultCosts.builder();
    }

    @Override
    Builder toBuilder();

    /// Builder for Costs.
    interface Builder extends Characteristics.Builder<Cost, Costs, Builder> {}
}
