package be.imgn.mtg.engine.oracle.domain;

/// A condition attached to an effect or ability — an "if" or "unless"
/// clause whose predicate text is captured verbatim until the grammar
/// refines it into structured sub-types. The [Kind] distinguishes
/// the two semantic flavors so consumers don't have to parse English:
/// [Kind#IF] means the effect resolves only when the predicate
/// holds; [Kind#UNLESS] is the negation — the effect is countered
/// unless the predicate is satisfied (e.g., by paying a cost).
public record Condition(Kind kind, String text) {

    /// Convenience: an `if`-style condition.
    public static Condition ifCondition(String text) {
        return new Condition(Kind.IF, text);
    }

    /// Convenience: an `unless`-style condition (the negation).
    public static Condition unlessCondition(String text) {
        return new Condition(Kind.UNLESS, text);
    }

    /// Convenience: an `as long as`-style condition — a continuous
    /// predicate that gates the enclosing effect for its whole
    /// active window.
    public static Condition asLongAs(String text) {
        return new Condition(Kind.AS_LONG_AS, text);
    }

    public enum Kind {
        /// "if \[predicate\]" — the enclosing effect resolves only when the
        /// predicate is true.
        IF,
        /// "unless \[predicate\]" — the enclosing effect is countered/does
        /// nothing if the predicate holds; it happens when the predicate
        /// is false (or the opponent declines to meet it).
        UNLESS,
        /// "as long as \[predicate\]" — continuous predicate that gates
        /// the enclosing continuous effect; distinct from IF (one-shot
        /// check at resolution) in that the predicate is re-checked
        /// while the effect is active.
        AS_LONG_AS
    }
}
