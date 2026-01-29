package be.imgn.mtg.engine.object;

import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.object.internal.DefaultSpell;

/// A spell on the stack ({@mtg.rule 112}).
///
/// A spell is a card on the stack. Casting a card puts it onto the stack as a new object.
/// A spell remains on the stack until it resolves, is countered, or otherwise leaves the stack.
///
/// When a spell resolves, if it's a permanent spell (artifact, creature, enchantment, or
/// planeswalker), it enters the battlefield as a [Permanent]. Instant and sorcery spells
/// have their effects and are then put into the graveyard. Spells are [StackObject]s.
///
/// @see Card
/// @see CardCopy
/// @see Permanent
/// @see StackObject
public non-sealed interface Spell extends TypedObject, StackObject {

    /// Returns the source of this spell.
    ///
    /// The source is either a [Card] or [CardCopy] that was cast to create this spell.
    ///
    /// @return the spell source, never null
    SpellSource source();

    /// Creates a spell builder from a card being cast.
    ///
    /// @param card the card being cast
    /// @param controller the player casting the spell
    /// @return a new builder initialized from the card
    static Builder fromCard(Card card, Player controller) {
        return DefaultSpell.fromCard(card, controller);
    }

    /// Creates a spell builder from a card copy being cast.
    ///
    /// @param copy the card copy being cast
    /// @param controller the player casting the spell
    /// @return a new builder initialized from the card copy
    static Builder fromCopy(CardCopy copy, Player controller) {
        return DefaultSpell.fromCopy(copy, controller);
    }

    /// Builder for [Spell].
    non-sealed interface Builder extends TypedObject.Builder<Spell, Builder> {}
}
