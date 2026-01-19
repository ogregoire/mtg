package be.imgn.mtg.engine.ability.internal.parser.effect;

import be.imgn.mtg.engine.ability.internal.parser.reference.Subject;

/// An effect that returns one or more objects to their owner's hand.
///
/// @param subject what to return to hand
public record ReturnToHandEffect(Subject subject) implements Effect {}
