package be.imgn.mtg.engine.object;

import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.object.internal.DefaultSpell;

/// A spell on the stack.
///
/// A spell is created when a player casts a card or card copy. Spells exist only
/// on the stack and can be responded to by other players. When a spell resolves,
/// it either becomes a [Permanent] (for permanent spells) or has its effect and
/// goes to the graveyard (for instants and sorceries).
///
/// Spells are [StackObject]s that can be countered and responded to.
///
/// @see Card
/// @see CardCopy
/// @see Permanent
/// @see StackObject
public non-sealed interface Spell extends GameObject, StackObject {

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
    non-sealed interface Builder extends GameObject.Builder<Spell, Builder> {}
}
