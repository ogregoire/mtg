package be.imgn.mtg.engine.ability.internal.parser.effect;

import be.imgn.mtg.engine.ability.internal.parser.reference.Subject;
import be.imgn.mtg.engine.ability.internal.parser.selector.Amount;
import be.imgn.mtg.engine.characteristics.CounterType;

/// An effect that puts counters on one or more objects.
///
/// @param amount how many counters
/// @param counterType the type of counter (e.g., "+1/+1", "loyalty", "charge")
/// @param subject what receives the counters
public record AddCountersEffect(Amount amount, CounterType counterType, Subject subject) implements Effect {}
