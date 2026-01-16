package be.imgn.mtg.engine.object;

import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.object.internal.DefaultCardCopy;

/// A copy of a card (not a token).
///
/// Card copies are created by effects that copy cards, such as "copy target instant
/// or sorcery spell". Unlike tokens, card copies maintain a reference to the original
/// card they were copied from.
///
/// Card copies are [SpellSource] objects that can be cast as spells.
///
/// @see Card
/// @see Spell
public non-sealed interface CardCopy extends GameObject, SpellSource {

    /// Returns the original card this is a copy of.
    ///
    /// @return the original card, never null
    Card original();

    /// Returns a new builder for CardCopy.
    ///
    /// @return a new builder instance
    static Builder builder() {
        return DefaultCardCopy.builder();
    }

    /// Builder for [CardCopy].
    non-sealed interface Builder extends GameObject.Builder<CardCopy, Builder> {

        /// Sets the owner of the card copy.
        ///
        /// @param owner the owning player
        /// @return this builder
        Builder owner(Player owner);

        /// Sets the controller of the card copy.
        ///
        /// @param controller the controlling player
        /// @return this builder
        Builder controller(Player controller);

        /// Sets the original card this is a copy of.
        ///
        /// @param original the original card
        /// @return this builder
        Builder original(Card original);
    }
}
