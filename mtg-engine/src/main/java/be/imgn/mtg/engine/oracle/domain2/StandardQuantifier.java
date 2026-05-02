package be.imgn.mtg.engine.oracle.domain2;

/// Bare-word quantifiers — determiners that don't carry a number.
/// [#ALL] is the canonical form for "all" / "each" / "every"
/// (semantically equivalent in MTG resolution; the rules don't
/// distinguish them).
///
/// "a" / "an" / "the" / "one" are NOT here — they map to
/// [Amount.Exact]`(1)`, since they all pick out exactly one element
/// with no functional difference at resolution time.
public enum StandardQuantifier implements Quantifier {
    /// "all" / "each" / "every" — the entire matching set.
    ALL,
    /// "no" — the empty selection. Used in conditions like "if you
    /// control no artifacts".
    NONE,
    /// "any number of" — zero or more matches; count chosen at
    /// resolution.
    ANY_NUMBER
}
