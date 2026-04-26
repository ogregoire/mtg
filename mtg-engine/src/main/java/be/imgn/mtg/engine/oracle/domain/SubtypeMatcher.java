package be.imgn.mtg.engine.oracle.domain;

import java.util.List;

/// Boolean predicate over an object's subtypes. Atoms cover the positive
/// head-noun form ("Goblin creature") and the negated qualifier form
/// ("non-Human creature"); [Any] and [All] combine them.
///
/// Used by [Selector.Qualifier.Subtypes] to express the subtype
/// constraint on a selector. Multi-occurrence negations like
/// "non-Vampire, non-Werewolf, non-Zombie creature" (Victim of Night)
/// fold into `All` via the qualifier-list merge step.
public sealed interface SubtypeMatcher {

    /// Matches objects of the given subtype.
    record Is(Subtype subtype) implements SubtypeMatcher {}

    /// Matches objects without the given subtype.
    record Not(Subtype subtype) implements SubtypeMatcher {}

    /// Disjunction — matches when *any* contained matcher matches.
    record Any(List<SubtypeMatcher> matchers) implements SubtypeMatcher {}

    /// Conjunction — matches when *every* contained matcher matches.
    /// Emitted by the qualifier-list merge step that folds multiple
    /// subtype qualifiers into one.
    record All(List<SubtypeMatcher> matchers) implements SubtypeMatcher {}
}
