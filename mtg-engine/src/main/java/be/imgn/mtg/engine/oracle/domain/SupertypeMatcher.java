package be.imgn.mtg.engine.oracle.domain;

import java.util.List;

/// Boolean predicate over an object's supertypes. Atoms cover positive
/// ("Legendary creature") and negated ("Nonlegendary creature") forms;
/// [Any] and [All] combine them.
///
/// Used by [Selector.Qualifier.Supertypes] to express the supertype
/// constraint on a selector — typically a single atom, occasionally an
/// `All` when multiple supertype predicates appear in the same clause
/// ("legendary, nonbasic land").
public sealed interface SupertypeMatcher {

    /// Matches objects of the given supertype.
    record Is(Supertype supertype) implements SupertypeMatcher {}

    /// Matches objects without the given supertype.
    record Not(Supertype supertype) implements SupertypeMatcher {}

    /// Disjunction — matches when *any* contained matcher matches.
    record Any(List<SupertypeMatcher> matchers) implements SupertypeMatcher {}

    /// Conjunction — matches when *every* contained matcher matches.
    /// Emitted by the qualifier-list merge step that folds multiple
    /// supertype qualifiers into one.
    record All(List<SupertypeMatcher> matchers) implements SupertypeMatcher {}

    SupertypeMatcher LEGENDARY = new Is(Supertype.LEGENDARY);
    SupertypeMatcher NONLEGENDARY = new Not(Supertype.LEGENDARY);
    SupertypeMatcher BASIC = new Is(Supertype.BASIC);
    SupertypeMatcher NONBASIC = new Not(Supertype.BASIC);
    SupertypeMatcher SNOW = new Is(Supertype.SNOW);
    SupertypeMatcher NONSNOW = new Not(Supertype.SNOW);
    SupertypeMatcher WORLD = new Is(Supertype.WORLD);
    SupertypeMatcher NONWORLD = new Not(Supertype.WORLD);
}
