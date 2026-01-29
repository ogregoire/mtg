package be.imgn.mtg.engine.object;

import be.imgn.mtg.engine.ability.Abilities;
import be.imgn.mtg.engine.ability.Ability;
import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.object.internal.DefaultEmblem;

/// An emblem in Magic: The Gathering ({@mtg.rule 114}).
///
/// An emblem is an object that has one or more abilities but no other characteristics.
/// Emblems are not permanents, and nothing can remove them from the command zone.
/// An emblem has no color, no mana cost, no name, no types, no power/toughness,
/// and no loyalty — only abilities.
///
/// Emblems are created by planeswalker abilities and exist in the command zone.
/// They are owned and controlled by the player who created them.
///
/// @see GameObject
public non-sealed interface Emblem extends GameObject {

    /// Returns the abilities of this emblem.
    ///
    /// @return the abilities, never null (may be empty)
    Abilities abilities();

    /// Returns a new builder for Emblem.
    ///
    /// @return a new builder instance
    static Builder builder() {
        return DefaultEmblem.builder();
    }

    /// Builder for [Emblem].
    interface Builder {

        /// Sets the owner of the emblem.
        ///
        /// @param owner the owning player
        /// @return this builder
        Builder owner(Player owner);

        /// Sets the controller of the emblem.
        ///
        /// @param controller the controlling player
        /// @return this builder
        Builder controller(Player controller);

        /// Adds an ability to the emblem.
        ///
        /// @param ability the ability to add
        /// @return this builder
        Builder addAbility(Ability ability);

        /// Sets all abilities, replacing any existing abilities.
        ///
        /// @param abilities the abilities
        /// @return this builder
        Builder abilities(Abilities abilities);

        /// Builds and returns the Emblem.
        ///
        /// @return the constructed emblem
        Emblem build();
    }
}
