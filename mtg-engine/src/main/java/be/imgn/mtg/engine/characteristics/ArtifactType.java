package be.imgn.mtg.engine.characteristics;

/// Artifact subtypes (rule 205.3g).
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
