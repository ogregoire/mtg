package be.imgn.mtg.engine.cost.internal;

import static java.util.Objects.requireNonNull;

import be.imgn.mtg.engine.cost.Cost;
import be.imgn.mtg.engine.selector.ObjectSelector;

/// A sacrifice cost ({@mtg.rule 118.8}).
///
/// @param selector the objects that can be sacrificed
public record SacrificeCost(ObjectSelector selector) implements Cost {
    /// Creates a new sacrifice cost.
    public SacrificeCost {
        requireNonNull(selector, "selector");
    }

    @Override
    public String description() {
        return "Sacrifice " + selector.typeMatcher();
    }
}
