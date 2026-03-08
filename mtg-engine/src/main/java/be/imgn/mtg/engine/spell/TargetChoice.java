package be.imgn.mtg.engine.spell;

import be.imgn.mtg.engine.ability.internal.parser.reference.Subject;
import be.imgn.mtg.engine.selector.Selectable;

/// A single target choice mapping a subject to a chosen target.
///
/// @param subject the effect's subject that requires targeting
/// @param target the chosen target (game object or player)
public record TargetChoice(Subject subject, Selectable target) {}
