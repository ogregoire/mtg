package be.imgn.mtg.engine.oracle.domain;

import java.util.List;

/// A condition attached to an effect or ability — an "if" / "unless" /
/// "as long as" clause. Modelled as a sealed type so the grammar can
/// migrate common shapes from the free-text [Predicate] fallback into
/// structured variants over time. The [Kind] distinguishes the
/// semantic flavour so consumers don't have to inspect English text:
/// [Kind#IF] means the effect resolves only when the predicate holds;
/// [Kind#UNLESS] is the negation — the effect is countered unless the
/// predicate is satisfied; [Kind#AS_LONG_AS] is a continuous predicate
/// gating the enclosing continuous effect.
public sealed interface Condition {

    Kind kind();

    /// Free-text predicate — the legacy fallback for shapes the grammar
    /// hasn't structured yet (Ertai's Trickery: "if it was kicked";
    /// Lava Blister: "unless its controller has ~ deal 6 damage to
    /// them.").
    record Predicate(Kind kind, String text) implements Condition {}

    /// "\[player\] both own\[s\] and control\[s\] \<subjects\>" — meld-gate
    /// condition (Gisela, the Broken Blade: "if you both own and
    /// control Gisela and a creature named Bruna, the Fading Light").
    /// `who` is the player; `targets` is the conjunction of objects
    /// whose ownership and control is being checked.
    record OwnsAndControls(Kind kind, Subject who, List<Subject> targets) implements Condition {}

    /// Convenience: an `if`-style free-text condition.
    static Condition ifCondition(String text) {
        return new Predicate(Kind.IF, text);
    }

    /// Convenience: an `unless`-style free-text condition (the negation).
    static Condition unlessCondition(String text) {
        return new Predicate(Kind.UNLESS, text);
    }

    /// Convenience: an `as long as`-style free-text condition — a
    /// continuous predicate that gates the enclosing effect for its
    /// whole active window.
    static Condition asLongAs(String text) {
        return new Predicate(Kind.AS_LONG_AS, text);
    }

    enum Kind {
        /// "if \[predicate\]" — the enclosing effect resolves only when
        /// the predicate is true.
        IF,
        /// "unless \[predicate\]" — the enclosing effect is countered /
        /// does nothing if the predicate holds; it happens when the
        /// predicate is false (or the opponent declines to meet it).
        UNLESS,
        /// "as long as \[predicate\]" — continuous predicate that
        /// gates the enclosing continuous effect; distinct from
        /// [#IF] (one-shot check at resolution) in that the predicate
        /// is re-checked while the effect is active.
        AS_LONG_AS
    }
}
