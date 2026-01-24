package be.imgn.mtg.engine.cost.internal;

import java.util.Objects;

import be.imgn.mtg.engine.ability.internal.parser.selector.ObjectSelector;
import be.imgn.mtg.engine.cost.Cost;

/// A reveal cost ({@mtg.rule 118.8}).
///
/// @param selector the objects to reveal
public record RevealCost(ObjectSelector selector) implements Cost {
    /// Creates a new reveal cost.
    public RevealCost {
        Objects.requireNonNull(selector, "selector");
    }

    @Override
    public String description() {
        return "Reveal " + selector.typeMatcher();
    }
}
