package be.imgn.mtg.engine.cost.internal;

import java.util.Objects;

import be.imgn.mtg.engine.ability.internal.parser.selector.Amount;
import be.imgn.mtg.engine.cost.Cost;

/// A mill cost ({@mtg.rule 701.13}).
///
/// @param amount the number of cards to mill
public record MillCost(Amount amount) implements Cost {
    /// Creates a new mill cost.
    public MillCost {
        Objects.requireNonNull(amount, "amount");
    }

    @Override
    public String description() {
        return "Mill " + describeAmount(amount);
    }

    private static String describeAmount(Amount amount) {
        return switch (amount) {
            case Amount.Exact e -> String.valueOf(e.value());
            case Amount.Reference r -> r.name();
            case Amount.Variable v -> "X";
        };
    }
}
