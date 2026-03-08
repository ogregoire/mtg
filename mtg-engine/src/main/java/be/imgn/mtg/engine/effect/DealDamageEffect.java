package be.imgn.mtg.engine.effect;

import be.imgn.mtg.engine.ability.internal.parser.reference.Subject;
import be.imgn.mtg.engine.ability.internal.parser.selector.Amount;

/// An effect that deals damage to one or more targets.
///
/// @param amount how much damage to deal
/// @param target what receives the damage
public record DealDamageEffect(Amount amount, Subject target) implements Effect {}
