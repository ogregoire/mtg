package be.imgn.mtg.engine.effect;

import be.imgn.mtg.engine.ability.internal.parser.reference.Subject;

/// An effect that untaps one or more permanents.
///
/// @param subject what to untap
public record UntapEffect(Subject subject) implements Effect {}
