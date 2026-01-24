package be.imgn.mtg.engine.cost.internal;

import java.util.Objects;

import be.imgn.mtg.engine.ability.internal.parser.selector.ObjectSelector;
import be.imgn.mtg.engine.cost.Cost;

/// A return-to-hand cost ({@mtg.rule 118.8}).
///
/// @param selector the objects to return to hand
public record ReturnToHandCost(ObjectSelector selector) implements Cost {
    /// Creates a new return-to-hand cost.
    public ReturnToHandCost {
        Objects.requireNonNull(selector, "selector");
    }

    @Override
    public String description() {
        return "Return " + selector.typeMatcher() + " to its owner's hand";
    }
}
