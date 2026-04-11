package be.imgn.mtg.engine.oracle;

import java.util.List;

import org.jspecify.annotations.Nullable;

/// Activation or casting cost.
public sealed interface Cost {

    record Mana(List<ManaSymbol> symbols) implements Cost {}

    record TapSelf() implements Cost {}

    record UntapSelf() implements Cost {}

    record Loyalty(int change) implements Cost {}

    record PayLife(Amount amount) implements Cost {}

    record SacrificePermanent(Selector what) implements Cost {}

    record DiscardCard(Selector what) implements Cost {}

    record TapPermanent(Selector what) implements Cost {}

    record ExilePermanent(Selector what, Zone.@Nullable Source from) implements Cost {
        ExilePermanent(Selector what) {
            this(what, null);
        }
    }

    record RemoveCounter(Amount count, CounterType type, Subject from) implements Cost {}

    record Compound(List<Cost> costs) implements Cost {}
}
