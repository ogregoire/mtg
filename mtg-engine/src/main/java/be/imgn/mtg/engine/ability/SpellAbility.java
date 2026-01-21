package be.imgn.mtg.engine.ability;

import java.util.List;

import be.imgn.mtg.engine.ability.internal.parser.effect.Effect;

/// A spell ability ({@mtg.rule 113.3a}).
///
/// Spell abilities are the instructions followed when an instant or sorcery spell resolves.
/// They may also appear on other objects (such as activated or triggered abilities that
/// instruct a player to cast a spell).
///
/// Unlike activated or triggered abilities, spell abilities don't exist independently;
/// they are the effect part of a spell on the stack.
///
/// @see Ability
public non-sealed interface SpellAbility extends Ability {

    /// Returns the effects that this spell ability produces when resolved.
    ///
    /// @return the list of effects, never null (may be empty)
    List<Effect> effects();
}
