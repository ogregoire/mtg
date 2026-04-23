package be.imgn.mtg.engine.oracle.domain;

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
    /// selector-based (`a creature you control`). `what` is a
    /// [Subject] so both forms share this type.
    record SacrificePermanent(Subject what) implements Cost {}

    /// Discard-card cost. Accepts either a self-reference (`this card`,
    /// `~`) or a selector (`a creature card you control`). `what`
    /// is a [Subject] so both forms share this type.
    record DiscardCard(Subject what, boolean atRandom) implements Cost {
        public DiscardCard(Subject what) {
            this(what, false);
        }
    }

    /// "Discard your hand" — discard every card in the player's hand as a
    /// cost (Null Brooch).
    enum DiscardHand implements Cost {
        DISCARD_HAND
    }

    record TapPermanent(Selector what) implements Cost {}

    /// Exile cost. Accepts either a self-reference (`this card`, `~`) or a
    /// selector (`a creature you control`). The optional `from` names
    /// the zone the object is exiled from ("from your hand", "from your
    /// graveyard") when different from the battlefield default.
    record Exile(Subject what, Zone.@Nullable Source from) implements Cost {
        public Exile(Subject what) {
            this(what, null);
        }

        public Exile withFrom(Zone.Source from) {
            return new Exile(what, from);
        }
    }

    record RemoveCounter(Amount count, CounterType type, Subject from) implements Cost {}

    /// "Return \[subject\] to its owner's hand" — bounce as activation cost
    /// (Broken Fall: "Return this enchantment to its owner's hand:
    /// Regenerate target creature."). `what` is typically a
    /// self-reference but the parser accepts any [Subject] for
    /// uniformity with other subject-bearing costs.
    record ReturnToHand(Subject what) implements Cost {}

    /// "Put a \[type\] counter on \[subject\]" — counter placement as
    /// activation cost (Devoted Druid: "Put a -1/-1 counter on this
    /// creature: Untap this creature."). Mirrors
    /// [Effect.AddCounters] but on the cost side.
    record AddCounter(Amount count, CounterType type, Subject on) implements Cost {}

    /// "Reveal \[N\] cards from your hand \[that share X\]?" — reveal as
    /// an activation cost (Illuminated Folio: "{1}, {T}, Reveal two
    /// cards from your hand that share a color: Draw a card.").
    /// `constraint` captures the optional "that share …" qualifier as
    /// free text until the grammar refines it into a structured
    /// characteristic; `null` for the bare reveal.
    record Reveal(Amount count, @Nullable String constraint) implements Cost {}

    record Compound(List<Cost> costs) implements Cost {}

    /// "A or B" — alternative cost (Bloodthorn Flail: "Equip—Pay {3} or
    /// discard a card."). Exactly one of the options must be paid.
    record Or(List<Cost> options) implements Cost {}
}
