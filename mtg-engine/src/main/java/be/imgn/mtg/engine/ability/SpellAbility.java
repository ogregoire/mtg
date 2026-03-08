package be.imgn.mtg.engine.ability;

import java.util.List;

import be.imgn.mtg.engine.effect.Effect;

/// A spell ability ({@mtg.rule 113.3a}).
///
/// Spell abilities are the instructions followed when an instant or sorcery spell resolves.
/// They are part of a spell on the stack, not independent abilities.
///
/// @param id the unique ability identifier
/// @param oracleText the original rules text
/// @param effects the effects that execute when the spell resolves
/// @see Ability
public record SpellAbility(AbilityId id, String oracleText, List<Effect> effects) implements Ability {}
