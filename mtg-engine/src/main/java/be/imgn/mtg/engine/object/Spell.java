package be.imgn.mtg.engine.object;

import be.imgn.mtg.engine.object.internal.DefaultSpell;

/// A spell on the stack.
public non-sealed interface Spell extends GameObject, StackObject {

    /// Returns the source of this spell.
    SpellSource source();

    /// Creates a spell builder from a card being cast.
    static Builder fromCard(Card card, Player controller) {
        return DefaultSpell.fromCard(card, controller);
    }

    /// Creates a spell builder from a card copy being cast.
    static Builder fromCopy(CardCopy copy, Player controller) {
        return DefaultSpell.fromCopy(copy, controller);
    }

    /// Builder for Spell.
    non-sealed interface Builder extends GameObject.Builder<Spell, Builder> {}
}
