package be.imgn.mtg.engine.ability.internal.parser.effect;

import java.util.Optional;

import be.imgn.mtg.engine.ability.internal.parser.reference.Subject;
import be.imgn.mtg.engine.ability.internal.parser.selector.Duration;

/// An effect that modifies power and/or toughness.
///
/// @param subject what gets modified
/// @param powerMod the power modification (can be negative)
/// @param toughnessMod the toughness modification (can be negative)
/// @param duration how long the effect lasts (empty for permanent)
public record ModifyPowerToughnessEffect(Subject subject, int powerMod, int toughnessMod, Optional<Duration> duration)
        implements Effect {}
