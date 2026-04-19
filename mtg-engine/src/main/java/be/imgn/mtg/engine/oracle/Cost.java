package be.imgn.mtg.engine.oracle;

import java.util.List;

import org.jspecify.annotations.Nullable;

/// Activation or casting cost.
public sealed interface Cost {

    record Mana(List<ManaSymbol> symbols) implements Cost {}

    enum TapSelf implements Cost {
        TAP_SELF
    }

    enum UntapSelf implements Cost {
        UNTAP_SELF
    }

    record Loyalty(int change) implements Cost {}

    record PayLife(Amount amount) implements Cost {}

    /// "Sacrifice [what]" — self-sacrifice (`~`, `this creature`) or
    /// selector-based (`a creature you control`). {@code what} is a
    /// {@link Subject} so both forms share this type.
    record SacrificePermanent(Subject what) implements Cost {}

    record DiscardCard(Selector what) implements Cost {}

    record TapPermanent(Selector what) implements Cost {}

    /// Exile cost. Accepts either a self-reference (`this card`, `~`) or a
    /// selector (`a creature you control`). The optional {@code from} names
    /// the zone the object is exiled from ("from your hand", "from your
    /// graveyard") when different from the battlefield default.
    record Exile(Subject what, Zone.@Nullable Source from) implements Cost {
        Exile(Subject what) {
            this(what, null);
        }

        public Exile withFrom(Zone.Source from) {
            return new Exile(what, from);
        }
    }

    record RemoveCounter(Amount count, CounterType type, Subject from) implements Cost {}

    record Compound(List<Cost> costs) implements Cost {}
}
