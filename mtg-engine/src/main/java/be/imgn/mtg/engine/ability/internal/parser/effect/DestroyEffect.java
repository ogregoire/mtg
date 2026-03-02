package be.imgn.mtg.engine.ability.internal.parser.effect;

import be.imgn.mtg.engine.ability.internal.parser.reference.Subject;

/// A destroy effect that destroys one or more permanents.
///
/// @param subject what to destroy
/// @param canBeRegenerated whether the destroyed permanent can be regenerated
public record DestroyEffect(Subject subject, boolean canBeRegenerated) implements Effect {}
