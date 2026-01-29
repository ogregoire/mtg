package be.imgn.mtg.engine.object;

import be.imgn.mtg.engine.ability.Ability;
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
/// Abilities on the stack do not have the full set of characteristics that [TypedObject]s
/// have. They have a name (derived from their source) but no mana cost, colors, types,
/// supertypes, subtypes, power, toughness, or loyalty.
///
/// @see Ability
/// @see StackObject
/// @see Spell
public non-sealed interface AbilityOnStack extends GameObject, StackObject {

    /// Returns the name of this ability on the stack.
    ///
    /// @return the name, never null
    String name();

    /// Returns the ability that was activated or triggered.
    ///
    /// @return the ability, never null
    Ability ability();

    /// Returns the source of this ability.
    ///
    /// The source is the typed object that has this ability.
    ///
    /// @return the source typed object, never null
    TypedObject source();

    /// Returns a new builder for AbilityOnStack with the given ability and source.
    ///
    /// The owner and controller are derived from the source.
    ///
    /// @param ability the ability being put on the stack
    /// @param source the typed object that has the ability
    /// @return a new builder instance
    static Builder from(Ability ability, TypedObject source) {
        return DefaultAbilityOnStack.from(ability, source);
    }

    /// Builder for [AbilityOnStack].
    interface Builder {

        /// Sets the name of the ability on the stack.
        ///
        /// @param name the name
        /// @return this builder
        Builder name(String name);

        /// Builds and returns the AbilityOnStack.
        ///
        /// @return the constructed ability on the stack
        AbilityOnStack build();
    }
}
