package be.imgn.mtg.engine.effect;

import be.imgn.mtg.engine.ability.internal.parser.reference.Subject;

/// An effect that counters one or more spells.
///
/// @param subject what spell(s) to counter
public record CounterSpellEffect(Subject subject) implements Effect {}
