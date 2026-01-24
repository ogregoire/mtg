package be.imgn.mtg.engine.cost.internal;

import be.imgn.mtg.engine.cost.Cost;

/// The untap symbol cost ({@mtg.rule 502.2}).
public record UntapCost() implements Cost {
    @Override
    public String description() {
        return "{Q}";
    }
}
