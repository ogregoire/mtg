package be.imgn.mtg.engine.cost.internal;

import static java.util.Objects.requireNonNull;

import be.imgn.mtg.engine.ability.internal.parser.selector.Amount;
import be.imgn.mtg.engine.cost.Cost;

/// An energy counter payment cost ({@mtg.rule 122.1b}).
///
/// @param amount the amount of energy to pay
public record EnergyCost(Amount amount) implements Cost {
    /// Creates a new energy cost.
    public EnergyCost {
        requireNonNull(amount, "amount");
    }

    @Override
    public String description() {
        return "Pay " + describeAmount(amount) + " {E}";
    }

    private static String describeAmount(Amount amount) {
        return switch (amount) {
            case Amount.Exact e -> String.valueOf(e.value());
            case Amount.Reference r -> r.name();
            case Amount.Variable v -> "X";
        };
    }
}
