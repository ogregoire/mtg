package be.imgn.mtg.engine.effect;

import be.imgn.mtg.engine.ability.internal.parser.reference.Subject;

/// An effect that taps one or more permanents.
///
/// @param subject what to tap
public record TapEffect(Subject subject) implements Effect {}
