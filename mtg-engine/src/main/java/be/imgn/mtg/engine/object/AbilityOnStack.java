package be.imgn.mtg.engine.object;

import be.imgn.mtg.engine.characteristics.Ability;
import be.imgn.mtg.engine.object.internal.DefaultAbilityOnStack;

/// An ability on the stack ({@mtg.rule 113}, {@mtg.rule 405}).
///
/// Activated and triggered abilities are objects on the stack. They are put on the stack
/// when activated or triggered, and remain there until they resolve, are countered, or
/// otherwise leave the stack.
///
/// Unlike spells, abilities on the stack are not cards and cannot be countered by effects
/// that counter spells. Only effects that specifically counter abilities can remove them.
/// Abilities are [StackObject]s with a source (the object the ability came from).
///
/// @see Ability
/// @see StackObject
/// @see Spell
public non-sealed interface AbilityOnStack extends GameObject, StackObject {

    /// Returns the ability that was activated or triggered.
    ///
    /// @return the ability, never null
    Ability ability();

    /// Returns the source of this ability.
    ///
    /// The source is the game object that has this ability.
    ///
    /// @return the source game object, never null
    GameObject source();

    /// Returns a new builder for AbilityOnStack with the given ability and source.
    ///
    /// The owner and controller are derived from the source.
    ///
    /// @param ability the ability being put on the stack
    /// @param source the game object that has the ability
    /// @return a new builder instance
    static Builder from(Ability ability, GameObject source) {
        return DefaultAbilityOnStack.from(ability, source);
    }

    /// Builder for [AbilityOnStack].
    non-sealed interface Builder extends GameObject.Builder<AbilityOnStack, Builder> {}
}
