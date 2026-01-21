package be.imgn.mtg.engine.object;

import org.jspecify.annotations.Nullable;

import be.imgn.mtg.engine.characteristics.Color;
import be.imgn.mtg.engine.characteristics.Colors;
import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.mana.ManaCost;
import be.imgn.mtg.engine.object.internal.DefaultCard;

/// A card in Magic: The Gathering ({@mtg.rule 108}).
///
/// A card is the standard component of the game. Cards can exist in the library, hand,
/// graveyard, exile, and command zone. The owner of a card is the player who started
/// the game with it in their deck.
///
/// When cast, a card becomes a [Spell] on the stack. When it enters the battlefield from
/// a permanent spell resolving, it becomes a [Permanent]. Cards are both [PermanentSource]
/// and [SpellSource].
///
/// @see Spell
/// @see Permanent
/// @see CardCopy
public non-sealed interface Card extends GameObject, PermanentSource, SpellSource {

    /// Returns the mana cost of this card.
    ///
    /// Some cards (like lands) have no mana cost.
    ///
    /// @return the mana cost, or null if the card has no mana cost
    @Nullable
    ManaCost manaCost();

    /// Returns the color indicator of this card, if any.
    ///
    /// The color indicator is a colored dot printed to the left of the type line
    /// that defines a card's color independently of its mana cost.
    ///
    /// @return the color indicator colors, never null (may be empty)
    Colors colorIndicator();

    /// Returns the rules text of this card.
    ///
    /// @return the rules text, never null (may be empty)
    String rulesText();

    /// Returns a new builder for Card.
    ///
    /// @return a new builder instance
    static Builder builder() {
        return DefaultCard.builder();
    }

    /// Builder for [Card].
    non-sealed interface Builder extends GameObject.Builder<Card, Builder> {

        /// Sets the owner of the card.
        ///
        /// @param owner the owning player
        /// @return this builder
        Builder owner(Player owner);

        /// Sets the controller of the card.
        ///
        /// @param controller the controlling player
        /// @return this builder
        Builder controller(Player controller);

        /// Sets the mana cost of the card.
        ///
        /// @param manaCost the mana cost, or null for no mana cost
        /// @return this builder
        Builder manaCost(ManaCost manaCost);

        /// Sets the color indicator of the card.
        ///
        /// @param colorIndicator the color indicator colors
        /// @return this builder
        Builder colorIndicator(Colors colorIndicator);

        /// Adds a color to the color indicator.
        ///
        /// @param color the color to add
        /// @return this builder
        Builder addColorIndicator(Color color);

        /// Sets the rules text of the card.
        ///
        /// @param rulesText the rules text
        /// @return this builder
        Builder rulesText(String rulesText);
    }
}
