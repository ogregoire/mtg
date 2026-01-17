package be.imgn.mtg.engine.characteristics;

/// Artifact subtypes, called artifact types ({@mtg.rule 205.3g}).
///
/// Artifact types are subtypes that are correlated to the artifact card type. An artifact can
/// have multiple artifact types. Equipment and Vehicle have special rules associated with them.
public enum ArtifactType implements Subtype {
    /// Attraction artifact type from Unfinity.
    ATTRACTION("Attraction"),
    /// Blood token artifact type.
    BLOOD("Blood"),
    /// Clue token artifact type, can be sacrificed to draw a card.
    CLUE("Clue"),
    /// Contraption artifact type from Unstable.
    CONTRAPTION("Contraption"),
    /// Equipment artifact type, can be attached to creatures.
    EQUIPMENT("Equipment"),
    /// Food token artifact type, can be sacrificed to gain life.
    FOOD("Food"),
    /// Fortification artifact type, can be attached to lands.
    FORTIFICATION("Fortification"),
    /// Gold token artifact type.
    GOLD("Gold"),
    /// Incubator token artifact type from Phyrexia.
    INCUBATOR("Incubator"),
    /// Junk token artifact type.
    JUNK("Junk"),
    /// Map token artifact type.
    MAP("Map"),
    /// Powerstone token artifact type.
    POWERSTONE("Powerstone"),
    /// Treasure token artifact type, can be sacrificed for mana.
    TREASURE("Treasure"),
    /// Vehicle artifact type, can become a creature when crewed.
    VEHICLE("Vehicle");

    private final String text;

    ArtifactType(String text) {
        this.text = text;
    }

    @Override
    public String text() {
        return text;
    }
}
