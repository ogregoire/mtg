package be.imgn.mtg.engine.oracle.domain;

import java.util.List;

/// Boolean predicate over an object's card types. Atoms cover the
/// positive head-noun form ("creature spell") and the negated qualifier
/// form ("noncreature spell"); [Any] and [All] combine them.
///
/// Used by [Selector.Qualifier.CardTypes] to express the card-type
/// constraint on a selector. Single-axis disjunctions like "creature or
/// planeswalker" become one `Any` matcher; multi-occurrence negations
/// like "noncreature, nonland spell" fold into `All` via the qualifier-
/// list merge step.
public sealed interface CardTypeMatcher {

    /// Matches objects of the given card type.
    record Is(CardType type) implements CardTypeMatcher {}

    /// Matches objects without the given card type.
    record Not(CardType type) implements CardTypeMatcher {}

    /// Disjunction — matches when *any* contained matcher matches.
    /// Emitted directly by the parser for "X or Y" oracle phrases.
    record Any(List<CardTypeMatcher> matchers) implements CardTypeMatcher {}

    /// Conjunction — matches when *every* contained matcher matches.
    /// Emitted by the qualifier-list merge step that folds multiple
    /// card-type qualifiers into one.
    record All(List<CardTypeMatcher> matchers) implements CardTypeMatcher {}

    // Positive card types
    CardTypeMatcher CREATURE = new Is(CardType.CREATURE);
    CardTypeMatcher ARTIFACT = new Is(CardType.ARTIFACT);
    CardTypeMatcher ENCHANTMENT = new Is(CardType.ENCHANTMENT);
    CardTypeMatcher LAND = new Is(CardType.LAND);
    CardTypeMatcher PLANESWALKER = new Is(CardType.PLANESWALKER);
    CardTypeMatcher BATTLE = new Is(CardType.BATTLE);
    CardTypeMatcher INSTANT = new Is(CardType.INSTANT);
    CardTypeMatcher SORCERY = new Is(CardType.SORCERY);
    CardTypeMatcher KINDRED = new Is(CardType.KINDRED);
    CardTypeMatcher DUNGEON = new Is(CardType.DUNGEON);

    // Negated card types — the common qualifier form
    CardTypeMatcher NONCREATURE = new Not(CardType.CREATURE);
    CardTypeMatcher NONARTIFACT = new Not(CardType.ARTIFACT);
    CardTypeMatcher NONENCHANTMENT = new Not(CardType.ENCHANTMENT);
    CardTypeMatcher NONLAND = new Not(CardType.LAND);
    CardTypeMatcher NONPLANESWALKER = new Not(CardType.PLANESWALKER);
}
