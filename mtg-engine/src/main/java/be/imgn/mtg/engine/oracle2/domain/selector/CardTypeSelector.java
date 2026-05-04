package be.imgn.mtg.engine.oracle2.domain.selector;

import static java.util.Objects.requireNonNull;

import be.imgn.mtg.engine.oracle2.domain.CardType;

/// Selects an object by its card type ({@mtg.rule 205.2}) —
/// "creature", "artifact", "land", etc. Single-axis negation
/// ("noncreature", "nonland") has its own [IsNot] arm rather than
/// going through `ObjectPropertySelector.Not(...)`. Boolean
/// composition ("creature or planeswalker") still goes through
/// `.AnyOf` / `.AllOf`.
public sealed interface CardTypeSelector extends CharacteristicSelector
        permits CardTypeSelector.Is, CardTypeSelector.IsNot, CardTypeSelector.SharesACardTypeWith {

    /// "[card-type]" — single positive card-type match. Example:
    /// "creature" → `new Is(CardType.CREATURE)`.
    record Is(CardType type) implements CardTypeSelector {
        public Is {
            requireNonNull(type);
        }
    }

    /// "non[card-type]" — single negative card-type match. Example:
    /// "noncreature" → `new IsNot(CardType.CREATURE)`.
    record IsNot(CardType type) implements CardTypeSelector {
        public IsNot {
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
}
