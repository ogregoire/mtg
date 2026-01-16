package be.imgn.mtg.engine.characteristics;

/// A subtype in Magic. All permitted implementations are enums.
public sealed interface Subtype
        permits ArtifactType, CreatureType, EnchantmentType, LandType, PlaneswalkerType, SpellType {

    /// Returns the text representation of this subtype.
    String text();
}
