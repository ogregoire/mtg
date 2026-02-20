package be.imgn.mtg.engine.cost.internal;

import java.util.Objects;

import be.imgn.mtg.engine.cost.Cost;
import be.imgn.mtg.engine.selector.ObjectSelector;

/// A tap-permanents cost ({@mtg.rule 118.8}).
///
/// @param selector the permanents to tap
public record TapPermanentsCost(ObjectSelector selector) implements Cost {
    /// Creates a new tap-permanents cost.
    public TapPermanentsCost {
        Objects.requireNonNull(selector, "selector");
    }

    @Override
    public String description() {
        return "Tap " + selector.typeMatcher();
    }
}
