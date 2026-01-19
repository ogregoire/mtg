package be.imgn.mtg.engine.ability.internal.parser.reference;

import java.util.Optional;

import be.imgn.mtg.engine.ability.internal.parser.selector.ObjectSelector;
import be.imgn.mtg.engine.ability.internal.parser.selector.TypeMatcher;

/// The subject of an effect (what the effect targets or affects).
public sealed interface Subject {

    /// Select objects matching the selector.
    record Select(ObjectSelector selector) implements Subject {}

    /// A pronoun reference to a previously mentioned object.
    record Pronoun(PronounType pronoun) implements Subject {}

    /// A "that" reference with optional type (e.g., "that creature").
    record ThatObject(Optional<TypeMatcher> typeMatcher) implements Subject {}
}
