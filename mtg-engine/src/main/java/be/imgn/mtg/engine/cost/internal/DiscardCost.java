package be.imgn.mtg.engine.cost.internal;

import static java.util.Objects.requireNonNull;

import be.imgn.mtg.engine.cost.Cost;
import be.imgn.mtg.engine.selector.ObjectSelector;

/// A discard cost ({@mtg.rule 118.8}).
///
/// @param selector the cards that can be discarded (quantity in selector's quantifier)
public record DiscardCost(ObjectSelector selector) implements Cost {
    /// Creates a new discard cost.
    public DiscardCost {
        requireNonNull(selector, "selector");
    }

    @Override
    public String description() {
        return "Discard " + selector.typeMatcher();
    }
}
