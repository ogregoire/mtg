package be.imgn.mtg.engine.effect;

import be.imgn.mtg.engine.ability.internal.parser.reference.Subject;

/// An exile effect that exiles one or more objects.
///
/// @param subject what to exile
public record ExileEffect(Subject subject) implements Effect {}
