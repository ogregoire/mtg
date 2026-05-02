package be.imgn.mtg.engine.oracle.domain2;

/// A numeric expression — used wherever oracle text states a count
/// (selection count, damage, life loss/gain, draw count, etc.).
/// Extends [Quantifier] so a numeric amount can also serve as the
/// count axis on a selection.
///
/// Variants:
/// - [Standard] for bare-word numerics (X, that-many)
/// - [Exact] / [UpTo] / [Range] for explicit numeric forms
///
/// Future variants (e.g., "twice X", "the number of [things]") get
/// added as new permitted records when oracle text needs them — not
/// pre-emptively.
public sealed interface Amount extends Quantifier permits Amount.Standard, Amount.Exact, Amount.UpTo, Amount.Range {

    /// Bare-word numeric atoms — values whose count is determined
    /// elsewhere (cost or context) rather than written as a literal.
    enum Standard implements Amount {
        /// "X" — variable bound by the spell or ability's cost
        /// ({@mtg.rule 107.3}).
        X,
        /// "that many" — back-reference to a count established
        /// earlier in the resolution (e.g., damage just dealt, cards
        /// just drawn).
        REFERENCE
    }

    /// Explicit count — "two", "three", etc.
    record Exact(int n) implements Amount {}

    /// "up to N" — between 0 and N inclusive.
    record UpTo(int n) implements Amount {}

    /// "N or M" / "between N and M" — inclusive range.
    record Range(int min, int max) implements Amount {}
}
