package be.imgn.mtg.engine.object;

import be.imgn.mtg.engine.characteristics.Ability;
import be.imgn.mtg.engine.object.internal.DefaultAbilityOnStack;

/// An ability on the stack.
///
/// When a triggered ability triggers or an activated ability is activated, it
/// goes on the stack as an [AbilityOnStack]. Like spells, abilities on the stack
/// can be responded to.
///
/// Abilities on the stack are [StackObject]s that can be responded to (but
/// generally cannot be countered except by specific effects).
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
