package be.imgn.mtg.engine.oracle.domain;

import java.util.List;

/// A condition attached to an effect or ability — an "if" / "unless" /
/// "as long as" clause. Modelled as a sealed type with structural
/// variants only; there is no free-text fallback. New oracle shapes
/// require a new typed variant rather than a String predicate. The
/// [Kind] distinguishes the semantic flavour: [Kind#IF] resolves the
/// effect only when the predicate holds; [Kind#UNLESS] is the
/// negation; [Kind#AS_LONG_AS] is a continuous predicate gating the
/// enclosing continuous effect.
public sealed interface Condition {

    Kind kind();

    /// "if you do" / "if they do" — back-reference to whether a
    /// preceding [Effect.Optional] action was actually taken
    /// (Inheritance: "you may pay {3}. If you do, draw a card."). No
    /// parameters; the referent is the most recent optional in the
    /// same resolution.
    enum YouDidIt implements Condition {
        YOU_DID_IT;

        @Override
        public Kind kind() {
            return Kind.IF;
        }
    }

    /// "\[player\] both own\[s\] and control\[s\] \<subjects\>" — meld-gate
    /// condition (rule 701.39, Gisela, the Broken Blade: "if you both
    /// own and control Gisela and a creature named Bruna, the Fading
    /// Light"). `who` is the player; `targets` is the conjunction of
    /// objects whose ownership and control is being checked.
    record OwnsAndControls(Kind kind, Subject who, List<Subject> targets) implements Condition {}

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
