package be.imgn.mtg.engine.oracle;

import java.util.List;

/// A subtype of a card type ({@mtg.rule 205.3}). The parser recognizes
/// subtypes of every card type the oracle grammar cares about; each is a
/// distinct enum so consumers can pattern-match on the family.
public sealed interface Subtype
        permits ArtifactType, BattleType, CreatureType, EnchantmentType, LandType, PlaneswalkerType, SpellType {

    /// Canonical singular form as it appears in oracle text (e.g., "Goblin",
    /// "Aura", "Plains").
    String text();

    /// Every text form this subtype takes in oracle text — always the
    /// singular {@link #text()}, plus the plural form when it differs
    /// ({@code "Goblin", "Goblins"}). Subtypes whose plural equals their
    /// singular (e.g., "Eldrazi") and planeswalker subtypes (proper names)
    /// return a single-element list.
    List<String> texts();
}
