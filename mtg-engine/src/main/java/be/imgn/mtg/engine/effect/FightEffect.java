package be.imgn.mtg.engine.effect;

import be.imgn.mtg.engine.ability.internal.parser.reference.Subject;

/// An effect that causes creatures to fight.
///
/// @param firstCreature the first creature in the fight
/// @param secondCreature the second creature in the fight
public record FightEffect(Subject firstCreature, Subject secondCreature) implements Effect {}
