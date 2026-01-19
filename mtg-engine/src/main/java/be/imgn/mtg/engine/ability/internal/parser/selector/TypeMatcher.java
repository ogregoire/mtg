package be.imgn.mtg.engine.ability.internal.parser.selector;

import java.util.List;

import be.imgn.mtg.engine.characteristics.Type;

/// Matches card types in oracle text.
public sealed interface TypeMatcher {

    /// Matches a single specific type.
    record Single(Type type) implements TypeMatcher {}

    /// Matches any of the given types (e.g., "artifact or enchantment").
    record Or(List<TypeMatcher> matchers) implements TypeMatcher {}

    /// Matches any permanent (no type restriction).
    record AnyPermanent() implements TypeMatcher {}

    /// Matches any spell (any card on the stack).
    record AnySpell() implements TypeMatcher {}

    /// Matches any legal target for damage (creature, player, or planeswalker).
    record AnyTarget() implements TypeMatcher {}
}
