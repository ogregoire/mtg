package be.imgn.mtg.engine.cost.internal;

import be.imgn.mtg.engine.cost.Cost;

/// A loyalty counter cost on a planeswalker ({@mtg.rule 606}).
///
/// @param amount the loyalty change (positive for +N, negative for -N)
public record LoyaltyCost(int amount) implements Cost {
    @Override
    public String description() {
        if (amount >= 0) {
            return "[+" + amount + "]";
        }
        return "[" + amount + "]";
    }

    @Override
    public boolean isLoyaltyCost() {
        return true;
    }
}
