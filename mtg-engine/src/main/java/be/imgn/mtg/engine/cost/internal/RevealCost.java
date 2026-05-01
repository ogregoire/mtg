package be.imgn.mtg.engine.cost.internal;

import static java.util.Objects.requireNonNull;

import be.imgn.mtg.engine.cost.Cost;
import be.imgn.mtg.engine.selector.ObjectSelector;

/// A reveal cost ({@mtg.rule 118.8}).
///
/// @param selector the objects to reveal
public record RevealCost(ObjectSelector selector) implements Cost {
    /// Creates a new reveal cost.
    public RevealCost {
        requireNonNull(selector, "selector");
    }

    @Override
    public String description() {
        return "Reveal " + selector.typeMatcher();
    }
}
