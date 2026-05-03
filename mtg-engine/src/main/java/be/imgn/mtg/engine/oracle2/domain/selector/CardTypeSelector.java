package be.imgn.mtg.engine.oracle.domain2.selector;

import static java.util.Objects.requireNonNull;

import be.imgn.mtg.engine.oracle.domain2.CardType;

/// Selects an object by its card type ({@mtg.rule 205.2}) —
/// "creature", "artifact", "land", etc. Negation goes through
/// `ObjectPropertySelector.Not(...)` ("noncreature", "nonland");
/// boolean composition through `.AnyOf` / `.AllOf`
/// ("creature or planeswalker", "non-creature non-land permanent").
public sealed interface CardTypeSelector extends CharacteristicSelector
        permits CardTypeSelector.Is, CardTypeSelector.SharesACardTypeWith {

    /// "[card-type]" — single positive card-type match. Example:
    /// "creature" → `new Is(CardType.CREATURE)`.
    record Is(CardType type) implements CardTypeSelector {
        public Is {
            requireNonNull(type);
        }
    }

    /// "shares a card type with X" — at least one card type in
    /// common with the referenced object (~34 cards).
    record SharesACardTypeWith(ObjectSelector with) implements CardTypeSelector {
        public SharesACardTypeWith {
            requireNonNull(with);
        }
    }

    /// "creature".
    CardTypeSelector CREATURE = new Is(CardType.CREATURE);
    /// "artifact".
    CardTypeSelector ARTIFACT = new Is(CardType.ARTIFACT);
    /// "enchantment".
    CardTypeSelector ENCHANTMENT = new Is(CardType.ENCHANTMENT);
    /// "land".
    CardTypeSelector LAND = new Is(CardType.LAND);
    /// "planeswalker".
    CardTypeSelector PLANESWALKER = new Is(CardType.PLANESWALKER);
    /// "battle".
    CardTypeSelector BATTLE = new Is(CardType.BATTLE);
    /// "instant".
    CardTypeSelector INSTANT = new Is(CardType.INSTANT);
    /// "sorcery".
    CardTypeSelector SORCERY = new Is(CardType.SORCERY);
    /// "kindred".
    CardTypeSelector KINDRED = new Is(CardType.KINDRED);
    /// "dungeon".
    CardTypeSelector DUNGEON = new Is(CardType.DUNGEON);
}
