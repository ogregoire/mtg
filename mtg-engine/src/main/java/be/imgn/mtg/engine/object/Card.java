package be.imgn.mtg.engine.object;

import org.jspecify.annotations.Nullable;

import be.imgn.mtg.engine.characteristics.Color;
import be.imgn.mtg.engine.characteristics.Colors;
import be.imgn.mtg.engine.characteristics.ManaCost;
import be.imgn.mtg.engine.object.internal.DefaultCard;

/// A card in Magic.
public non-sealed interface Card extends GameObject, PermanentSource, SpellSource {

    /// Returns the mana cost of this card.
    @Nullable
    ManaCost manaCost();

    /// Returns the color indicator of this card, if any.
    Colors colorIndicator();

    /// Returns the rules text of this card.
    String rulesText();

    /// Returns a new builder for Card.
    static Builder builder() {
        return DefaultCard.builder();
    }

    /// Builder for Card.
    non-sealed interface Builder extends GameObject.Builder<Card, Builder> {

        Builder owner(Player owner);

        Builder controller(Player controller);

        Builder manaCost(ManaCost manaCost);

        Builder colorIndicator(Colors colorIndicator);

        Builder addColorIndicator(Color color);

        Builder rulesText(String rulesText);
    }
}
