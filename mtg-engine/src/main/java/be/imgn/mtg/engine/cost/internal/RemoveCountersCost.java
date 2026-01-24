package be.imgn.mtg.engine.cost.internal;

import java.util.Objects;

import be.imgn.mtg.engine.ability.internal.parser.reference.Subject;
import be.imgn.mtg.engine.ability.internal.parser.selector.Amount;
import be.imgn.mtg.engine.characteristics.CounterType;
import be.imgn.mtg.engine.cost.Cost;

/// A remove counters cost ({@mtg.rule 118.8}).
///
/// @param amount the number of counters to remove
/// @param counterType the type of counter to remove
/// @param subject the object to remove counters from
public record RemoveCountersCost(Amount amount, CounterType counterType, Subject subject) implements Cost {
    /// Creates a new remove counters cost.
    public RemoveCountersCost {
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(counterType, "counterType");
        Objects.requireNonNull(subject, "subject");
    }

    @Override
    public String description() {
        return "Remove " + describeAmount(amount) + " " + counterType.text() + " counter(s)";
    }

    private static String describeAmount(Amount amount) {
        return switch (amount) {
            case Amount.Exact e -> String.valueOf(e.value());
            case Amount.Reference r -> r.name();
            case Amount.Variable v -> "X";
        };
    }
}
