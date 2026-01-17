package be.imgn.mtg.engine.characteristics;

/// A subtype in Magic ({@mtg.rule 205.3}).
///
/// Subtypes are printed after a long dash following the card type. Each card type has its own
/// set of possible subtypes. This sealed interface is implemented by the various subtype enums.
public sealed interface Subtype
        permits ArtifactType, CreatureType, EnchantmentType, LandType, PlaneswalkerType, SpellType {

    /// Returns the text representation of this subtype.
    ///
    /// @return the subtype text
    String text();
}
