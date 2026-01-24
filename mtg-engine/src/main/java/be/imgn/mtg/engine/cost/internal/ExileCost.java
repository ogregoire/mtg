package be.imgn.mtg.engine.cost.internal;

import java.util.Locale;
import java.util.Objects;

import be.imgn.mtg.engine.ability.internal.parser.selector.ObjectSelector;
import be.imgn.mtg.engine.cost.Cost;
import be.imgn.mtg.engine.zone.ZoneType;

/// An exile cost ({@mtg.rule 118.8}).
///
/// @param sourceZone the zone to exile from
/// @param selector the objects that can be exiled
public record ExileCost(ZoneType sourceZone, ObjectSelector selector) implements Cost {
    /// Creates a new exile cost.
    public ExileCost {
        Objects.requireNonNull(sourceZone, "sourceZone");
        Objects.requireNonNull(selector, "selector");
    }

    @Override
    public String description() {
        return "Exile " + selector.typeMatcher() + " from " + sourceZone.name().toLowerCase(Locale.ROOT);
    }
}
