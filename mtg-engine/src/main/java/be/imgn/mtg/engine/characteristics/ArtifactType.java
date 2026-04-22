package be.imgn.mtg.engine.characteristics;

/// Artifact subtypes, called artifact types ({@mtg.rule 205.3g}).
///
/// Artifact types are subtypes that are correlated to the artifact card type. An artifact can
/// have multiple artifact types. Equipment and Vehicle have special rules associated with them.
public enum ArtifactType implements Subtype {
    /// Attraction artifact type from Unfinity.
    ATTRACTION,
    /// Blood token artifact type.
    BLOOD,
    /// Bobblehead artifact type.
    BOBBLEHEAD,
    /// Book artifact type (e.g., Spellbook).
    BOOK,
    /// Clue token artifact type, can be sacrificed to draw a card.
    CLUE,
    /// Contraption artifact type from Unstable.
    CONTRAPTION,
    /// Equipment artifact type, can be attached to creatures.
    EQUIPMENT,
    /// Food token artifact type, can be sacrificed to gain life.
    FOOD,
    /// Fortification artifact type, can be attached to lands.
    FORTIFICATION,
    /// Gold token artifact type.
    GOLD,
    /// Incubator token artifact type from Phyrexia.
    INCUBATOR,
    /// Infinity artifact type.
    INFINITY,
    /// Junk token artifact type.
    JUNK,
    /// Lander artifact type.
    LANDER,
    /// Map token artifact type.
    MAP,
    /// Mutagen artifact type.
    MUTAGEN,
    /// Powerstone token artifact type.
    POWERSTONE,
    /// Spacecraft artifact type.
    SPACECRAFT,
    /// Stone artifact type.
    STONE,
    /// Treasure token artifact type, can be sacrificed for mana.
    TREASURE,
    /// Vehicle artifact type, can become a creature when crewed.
    VEHICLE
}
