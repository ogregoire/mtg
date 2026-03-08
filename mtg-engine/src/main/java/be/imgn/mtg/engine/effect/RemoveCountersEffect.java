package be.imgn.mtg.engine.effect;

import be.imgn.mtg.engine.ability.internal.parser.reference.Subject;
import be.imgn.mtg.engine.ability.internal.parser.selector.Amount;
import be.imgn.mtg.engine.characteristics.CounterType;

/// An effect that removes counters from one or more objects.
///
/// @param amount how many counters to remove
/// @param counterType the type of counter (e.g., "+1/+1", "loyalty", "charge")
/// @param subject what loses the counters
public record RemoveCountersEffect(Amount amount, CounterType counterType, Subject subject) implements Effect {}
