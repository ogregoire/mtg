package be.imgn.mtg.engine.oracle.domain;

import java.util.List;

/// Boolean predicate over an object's type characteristics. Unifies
/// the four type axes — card type ([CardType]), subtype ([Subtype]),
/// supertype ([Supertype]), and game-object class ([GameObjectType]) —
/// plus the [ObjectDesignation] tag, into one matcher tree. `Not`/`All`/`Any`
/// combinators stack the leaves into the boolean shapes oracle text
/// admits.
///
/// Used by [Condition.IsType] to express "X is a creature" / "X
/// isn't a Goblin" / "X is a creature card" type-check predicates,
/// where a single condition can range across multiple axes.
public sealed interface TypeMatcher {

    /// "is a [card-type]" — e.g., "is a creature".
    record IsCardType(CardType type) implements TypeMatcher {}

    /// "is a [subtype]" — e.g., "is a Goblin".
    record IsSubtype(Subtype subtype) implements TypeMatcher {}

    /// "is a [supertype]" — e.g., "is a snow permanent".
    record IsSupertype(Supertype supertype) implements TypeMatcher {}

    /// "is a [game-object]" — e.g., "is a card", "is a token".
    record IsGameObject(GameObjectType type) implements TypeMatcher {}

    /// "is a [designation]" — e.g., "is a commander", "is your Ring-bearer".
    record HasDesignation(ObjectDesignation designation) implements TypeMatcher {}

    /// "isn't [matcher]" — negation.
    record Not(TypeMatcher matcher) implements TypeMatcher {}

    /// Conjunction — matches when *every* contained matcher matches.
    /// Compound type checks like "is a creature card" fold into `All`.
    record All(List<TypeMatcher> matchers) implements TypeMatcher {}

    /// Disjunction — matches when *any* contained matcher matches.
    record Any(List<TypeMatcher> matchers) implements TypeMatcher {}

    // Card type leaves
    TypeMatcher CREATURE = new IsCardType(CardType.CREATURE);
    TypeMatcher ARTIFACT = new IsCardType(CardType.ARTIFACT);
    TypeMatcher ENCHANTMENT = new IsCardType(CardType.ENCHANTMENT);
    TypeMatcher LAND = new IsCardType(CardType.LAND);
    TypeMatcher PLANESWALKER = new IsCardType(CardType.PLANESWALKER);
    TypeMatcher BATTLE = new IsCardType(CardType.BATTLE);
    TypeMatcher INSTANT = new IsCardType(CardType.INSTANT);
    TypeMatcher SORCERY = new IsCardType(CardType.SORCERY);
    TypeMatcher KINDRED = new IsCardType(CardType.KINDRED);
    TypeMatcher DUNGEON = new IsCardType(CardType.DUNGEON);

    // Negated card types — the common qualifier form
    TypeMatcher NONCREATURE = new Not(CREATURE);
    TypeMatcher NONARTIFACT = new Not(ARTIFACT);
    TypeMatcher NONENCHANTMENT = new Not(ENCHANTMENT);
    TypeMatcher NONLAND = new Not(LAND);
    TypeMatcher NONPLANESWALKER = new Not(PLANESWALKER);

    // Supertype leaves
    TypeMatcher LEGENDARY = new IsSupertype(Supertype.LEGENDARY);
    TypeMatcher NONLEGENDARY = new Not(LEGENDARY);
    TypeMatcher BASIC = new IsSupertype(Supertype.BASIC);
    TypeMatcher NONBASIC = new Not(BASIC);
    TypeMatcher SNOW = new IsSupertype(Supertype.SNOW);
    TypeMatcher NONSNOW = new Not(SNOW);
    TypeMatcher WORLD = new IsSupertype(Supertype.WORLD);
    TypeMatcher NONWORLD = new Not(WORLD);
}
