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

    /// "Untap \<selector\>" — additional cost requiring untapping a
    /// chosen permanent (Benthic Explorers: "{T}, Untap a tapped land
    /// an opponent controls: Add one mana of any type that land could
    /// produce."). Distinct from [UntapSelf] (the `{Q}` symbol), which
    /// always refers to the source itself.
    record UntapPermanent(Selector what) implements Cost {}

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

    /// "Reveal \<what\> from your hand \[\<constraint\>\]?" — reveal
    /// as an activation or additional cost. `what` is a typed
    /// selector whose [Selector.Quantifier] carries the count
    /// (one / N / X) and whose qualifiers + [GameObjectType] narrow
    /// the revealed object. `constraint`, when present, narrows the
    /// revealed set further (Illuminated Folio: "Reveal two cards
    /// from your hand that share a color." →
    /// [SharedTrait#COLOR]).
    record Reveal(Subject what, @Nullable Constraint constraint) implements Cost {
        public Reveal(Subject what) {
            this(what, null);
        }

        public Reveal withConstraint(Constraint constraint) {
            return new Reveal(what, constraint);
        }

        /// A typed predicate over the revealed set. Sealed so each
        /// new printed constraint shape gets its own variant rather
        /// than a free-text fallback.
        public sealed interface Constraint permits SharedTrait {}
    }

    /// "share a \<trait\>" — every revealed object has a single
    /// shared value of the named trait. Currently scoped to the one
    /// card that uses it (Illuminated Folio, [#COLOR]); add new
    /// variants as oracle text introduces them.
    enum SharedTrait implements Reveal.Constraint {
        /// "share a color" — Illuminated Folio.
        COLOR
    }

    /// "Put \[what\] on \[top|bottom\] of \[possessive\] library." —
    /// activation-cost variant that moves a card from a zone (typically
    /// the hand) onto a position in the library (Leashling: "Put a card
    /// from your hand on top of your library: Return this creature to
    /// its owner's hand.").
    record PutOnLibrary(Subject what, Zone.@Nullable Source from, Position position) implements Cost {
        public PutOnLibrary(Subject what, Position position) {
            this(what, null, position);
        }

        public enum Position {
            TOP,
            BOTTOM
        }
    }

    /// "Mill N card(s)" — mill cost (Deranged Assistant: "{T}, Mill a card: Add {C}").
    record Mill(Amount amount) implements Cost {}

    /// "A, B, …" — multi-part cost; every component must be paid
    /// (e.g., "{1}, {T}, sacrifice a creature"). Mirrors
    /// [#AnyOf] for the all-of side of the same dichotomy.
    record AllOf(List<Cost> costs) implements Cost {}

    /// "A or B" — alternative cost (Bloodthorn Flail: "Equip—Pay {3} or
    /// discard a card."). Exactly one of the options must be paid.
    record AnyOf(List<Cost> options) implements Cost {}
}
