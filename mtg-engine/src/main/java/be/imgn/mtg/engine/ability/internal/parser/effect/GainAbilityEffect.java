package be.imgn.mtg.engine.ability.internal.parser.effect;

import java.util.Optional;

import be.imgn.mtg.engine.ability.internal.parser.reference.Subject;
import be.imgn.mtg.engine.ability.internal.parser.selector.Duration;

/// An effect that grants an ability to one or more objects.
///
/// @param subject what gains the ability
/// @param ability the ability text (e.g., "flying", "haste", "hexproof")
/// @param duration how long the effect lasts (empty for permanent)
public record GainAbilityEffect(Subject subject, String ability, Optional<Duration> duration) implements Effect {}
