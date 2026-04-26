package be.imgn.mtg.engine.oracle.domain;

import java.util.List;

/// Boolean predicate over an object's type characteristics. Unifies
/// the four type axes — card type ([CardType]), subtype ([Subtype]),
/// supertype ([Supertype]), and game-object class ([GameObjectType]) —
/// plus the [Role] tag, into one matcher tree. `Not`/`All`/`Any`
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

    /// "is a [role]" — e.g., "is a commander".
    record IsRole(Role role) implements TypeMatcher {}

    /// "isn't [matcher]" — negation.
    record Not(TypeMatcher matcher) implements TypeMatcher {}

    /// Conjunction — matches when *every* contained matcher matches.
    /// Compound type checks like "is a creature card" fold into `All`.
    record All(List<TypeMatcher> matchers) implements TypeMatcher {}

    /// Disjunction — matches when *any* contained matcher matches.
    record Any(List<TypeMatcher> matchers) implements TypeMatcher {}
}
