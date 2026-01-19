package be.imgn.mtg.engine.ability.internal.parser.effect;

import java.util.Optional;

import be.imgn.mtg.engine.ability.internal.parser.reference.Subject;
import be.imgn.mtg.engine.ability.internal.parser.selector.Duration;

/// An effect that gains control of one or more permanents.
///
/// @param subject what to gain control of
/// @param duration how long control lasts (empty for permanent)
public record GainControlEffect(Subject subject, Optional<Duration> duration) implements Effect {}
