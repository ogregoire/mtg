package be.imgn.mtg.engine.cost.internal;

import be.imgn.mtg.engine.cost.Cost;

/// The tap symbol cost ({@mtg.rule 118.8}).
public record TapCost() implements Cost {
    @Override
    public String description() {
        return "{T}";
    }
}
