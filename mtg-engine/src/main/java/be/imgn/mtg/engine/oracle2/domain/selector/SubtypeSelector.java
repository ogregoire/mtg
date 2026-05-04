package be.imgn.mtg.engine.oracle2.domain.selector;

import static java.util.Objects.requireNonNull;

import be.imgn.mtg.engine.oracle2.domain.Subtype;

/// Selects an object by its subtype ({@mtg.rule 205.3}) — creature
/// types ("Goblin"), enchantment types ("Aura"), land types
/// ("Forest"), spell types ("Arcane"), etc. Single-axis negation
/// ("non-Human creature") has its own [IsNot] arm rather than going
/// through `ObjectPropertySelector.Not(...)`.
public sealed interface SubtypeSelector extends CharacteristicSelector
        permits SubtypeSelector.Is, SubtypeSelector.IsNot, SubtypeSelector.SharesACreatureTypeWith {

    /// "[subtype]" — single positive subtype match. Works for any
    /// [Subtype] family — creature, land, enchantment, artifact,
    /// planeswalker, spell, battle. Example: "Goblin creature" →
    /// `AllOf(new CardTypeSelector.Is(CardType.CREATURE), new Is(CreatureType.GOBLIN))`.
    record Is(Subtype subtype) implements SubtypeSelector {
        public Is {
            requireNonNull(subtype);
        }
    }

    /// "non-[subtype]" — single negative subtype match. Example:
    /// "non-Human creature" →
    /// `AllOf(new CardTypeSelector.Is(CardType.CREATURE), new IsNot(CreatureType.HUMAN))`.
    record IsNot(Subtype subtype) implements SubtypeSelector {
        public IsNot {
            requireNonNull(subtype);
        }
    }

    /// "shares a creature type with X" — at least one creature
    /// subtype in common with the referenced object (~54 cards;
    /// e.g., Reaper of the Wilds, Gilt-Leaf Archdruid). Specific to
    /// creature types — "shares a land type" (2 cards) and "shares a
    /// subtype" (0 cards) are too rare to model now.
    record SharesACreatureTypeWith(ObjectSelector with) implements SubtypeSelector {
        public SharesACreatureTypeWith {
            requireNonNull(with);
        }
    }
}
