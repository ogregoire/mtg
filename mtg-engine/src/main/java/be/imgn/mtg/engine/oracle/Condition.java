package be.imgn.mtg.engine.oracle;

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

    public enum Kind {
        /// "if \[predicate\]" — the enclosing effect resolves only when the
        /// predicate is true.
        IF,
        /// "unless \[predicate\]" — the enclosing effect is countered/does
        /// nothing if the predicate holds; it happens when the predicate
        /// is false (or the opponent declines to meet it).
        UNLESS
    }
}
