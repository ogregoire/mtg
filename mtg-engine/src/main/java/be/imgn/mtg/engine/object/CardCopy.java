package be.imgn.mtg.engine.object;

import be.imgn.mtg.engine.object.internal.DefaultCardCopy;

/// A copy of a card (not a token).
public non-sealed interface CardCopy extends GameObject, SpellSource {

    /// Returns the original card this is a copy of.
    Card original();

    /// Returns a new builder for CardCopy.
    static Builder builder() {
        return DefaultCardCopy.builder();
    }

    /// Builder for CardCopy.
    non-sealed interface Builder extends GameObject.Builder<CardCopy, Builder> {

        Builder owner(Player owner);

        Builder controller(Player controller);

        Builder original(Card original);
    }
}
