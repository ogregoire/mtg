package be.imgn.mtg.engine.cost;

import java.util.stream.Collector;

import be.imgn.mtg.engine.characteristics.internal.Characteristics;
import be.imgn.mtg.engine.cost.internal.DefaultCosts;

/// A collection of costs for a game object or ability ({@mtg.rule 118}).
///
/// Multiple costs may need to be paid to cast a spell or activate an ability. All costs must
/// be paid in full before the spell or ability is considered cast or activated.
public interface Costs extends Characteristics<Cost> {

    /// Returns an empty Costs collection.
    ///
    /// @return an empty collection
    static Costs empty() {
        return DefaultCosts.empty();
    }

    /// Returns a Costs collection containing the specified costs.
    ///
    /// @param costs the costs to include
    /// @return a collection containing the costs
    static Costs of(Cost... costs) {
        return DefaultCosts.of(costs);
    }

    /// Returns a new builder for Costs.
    ///
    /// @return a new builder
    static Builder builder() {
        return DefaultCosts.builder();
    }

    /// Returns a Collector that accumulates Cost elements into a Costs collection.
    ///
    /// @return a Collector for Costs
    static Collector<Cost, ?, Costs> toCosts() {
        return DefaultCosts.collector();
    }

    @Override
    Builder toBuilder();

    /// Builder for Costs.
    interface Builder extends Characteristics.Builder<Cost, Costs, Builder> {}
}
