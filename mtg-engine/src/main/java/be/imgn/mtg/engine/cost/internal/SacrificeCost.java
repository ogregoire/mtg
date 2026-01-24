package be.imgn.mtg.engine.cost.internal;

import java.util.Objects;

import be.imgn.mtg.engine.ability.internal.parser.selector.ObjectSelector;
import be.imgn.mtg.engine.cost.Cost;

/// A sacrifice cost ({@mtg.rule 118.8}).
///
/// @param selector the objects that can be sacrificed
public record SacrificeCost(ObjectSelector selector) implements Cost {
    /// Creates a new sacrifice cost.
    public SacrificeCost {
        Objects.requireNonNull(selector, "selector");
    }

    @Override
    public String description() {
        return "Sacrifice " + selector.typeMatcher();
    }
}
