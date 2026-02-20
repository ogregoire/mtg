package be.imgn.mtg.engine.ability.internal.parser.effect;

import java.util.Optional;

import be.imgn.mtg.engine.selector.TypeMatcher;

/// An effect that searches a library for a card.
///
/// @param cardType optional type restriction for the search (empty for "a card")
public record SearchLibraryEffect(Optional<TypeMatcher> cardType) implements Effect {}
