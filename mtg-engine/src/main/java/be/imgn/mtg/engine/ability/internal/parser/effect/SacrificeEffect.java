package be.imgn.mtg.engine.ability.internal.parser.effect;

import be.imgn.mtg.engine.ability.internal.parser.reference.Subject;

/// A sacrifice effect that requires sacrificing one or more permanents.
///
/// @param subject what to sacrifice
public record SacrificeEffect(Subject subject) implements Effect {}
