package be.imgn.mtg.engine.cost.internal;

import java.util.Objects;

import be.imgn.mtg.engine.ability.internal.parser.selector.ObjectSelector;
import be.imgn.mtg.engine.cost.Cost;

/// A discard cost ({@mtg.rule 118.8}).
///
/// @param selector the cards that can be discarded (quantity in selector's quantifier)
public record DiscardCost(ObjectSelector selector) implements Cost {
    /// Creates a new discard cost.
    public DiscardCost {
        Objects.requireNonNull(selector, "selector");
    }

    @Override
    public String description() {
        return "Discard " + selector.typeMatcher();
    }
}
