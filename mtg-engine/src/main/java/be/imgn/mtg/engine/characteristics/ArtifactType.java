package be.imgn.mtg.engine.characteristics;

/// Artifact subtypes, called artifact types ({@mtg.rule 205.3g}).
///
/// Artifact types are subtypes that are correlated to the artifact card type. An artifact can
/// have multiple artifact types. Equipment and Vehicle have special rules associated with them.
public enum ArtifactType implements Subtype {
    ATTRACTION("Attraction"),
    BLOOD("Blood"),
    CLUE("Clue"),
    CONTRAPTION("Contraption"),
    EQUIPMENT("Equipment"),
    FOOD("Food"),
    FORTIFICATION("Fortification"),
    GOLD("Gold"),
    INCUBATOR("Incubator"),
    JUNK("Junk"),
    MAP("Map"),
    POWERSTONE("Powerstone"),
    TREASURE("Treasure"),
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
