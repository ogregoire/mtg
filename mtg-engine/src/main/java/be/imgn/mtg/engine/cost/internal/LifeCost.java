package be.imgn.mtg.engine.cost.internal;

import static java.util.Objects.requireNonNull;

import be.imgn.mtg.engine.ability.internal.parser.selector.Amount;
import be.imgn.mtg.engine.cost.Cost;

/// A life payment cost ({@mtg.rule 118.8}).
///
/// @param amount the amount of life to pay
public record LifeCost(Amount amount) implements Cost {
    /// Creates a new life cost.
    public LifeCost {
        requireNonNull(amount, "amount");
    }

    @Override
    public String description() {
        return "Pay " + describeAmount(amount) + " life";
    }

    private static String describeAmount(Amount amount) {
        return switch (amount) {
            case Amount.Exact e -> String.valueOf(e.value());
            case Amount.Reference r -> r.name();
            case Amount.Variable v -> "X";
        };
    }
}
