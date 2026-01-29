package be.imgn.mtg.engine.object;

import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.object.internal.DefaultCardCopy;

/// A copy of a card ({@mtg.rule 707}).
///
/// When an effect copies a spell or card, a new object is created with the same copiable
/// values as the original. Copiable values are the values printed on the object, as modified
/// by other copy effects and certain other effects.
///
/// Card copies are distinct from tokens. When a copy of a spell resolves, it's not put into
/// any zone; it simply ceases to exist. Card copies are [SpellSource] objects that can be cast.
///
/// @see Card
/// @see Spell
public non-sealed interface CardCopy extends TypedObject, SpellSource {

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
    non-sealed interface Builder extends TypedObject.Builder<CardCopy, Builder> {

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
