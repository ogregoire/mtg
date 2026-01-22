package be.imgn.mtg.engine.mana;

import be.imgn.mtg.engine.ability.internal.parser.effect.Effect;

/// Sealed interface for all mana-adding effects.
///
/// This represents the different ways mana can be added to a player's pool:
/// - Exact mana symbols like "Add {G}{G}."
/// - Selection from options like "Add {U} or {B}."
/// - Any color combination like "Add two mana of any color."
public sealed interface AddManaEffect extends Effect
        permits AddExactManaEffect, AddManaSelectionEffect, AddManaCombinationEffect {}
