package be.imgn.mtg.engine.oracle.domain2.selector;

import static java.util.Objects.requireNonNull;

import be.imgn.mtg.engine.oracle.domain2.BasicLandType;
import be.imgn.mtg.engine.oracle.domain2.Subtype;

/// Selects an object by its subtype ({@mtg.rule 205.3}) — creature
/// types ("Goblin"), enchantment types ("Aura"), land types
/// ("Forest"), spell types ("Arcane"), etc. Negation goes through
/// `ObjectPropertySelector.Not(...)` ("non-Human creature").
public sealed interface SubtypeSelector extends CharacteristicSelector
        permits SubtypeSelector.Is, SubtypeSelector.SharesACreatureTypeWith {

    /// "[subtype]" — single positive subtype match. Works for any
    /// [Subtype] family — creature, land, enchantment, artifact,
    /// planeswalker, spell, battle. Example: "Goblin creature" →
    /// `AllOf(CardTypeSelector.CREATURE, new Is(CreatureType.GOBLIN))`.
    record Is(Subtype subtype) implements SubtypeSelector {
        public Is {
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

    /// "Plains".
    SubtypeSelector PLAINS = new Is(BasicLandType.PLAINS);
    /// "Island".
    SubtypeSelector ISLAND = new Is(BasicLandType.ISLAND);
    /// "Swamp".
    SubtypeSelector SWAMP = new Is(BasicLandType.SWAMP);
    /// "Mountain".
    SubtypeSelector MOUNTAIN = new Is(BasicLandType.MOUNTAIN);
    /// "Forest".
    SubtypeSelector FOREST = new Is(BasicLandType.FOREST);
}
