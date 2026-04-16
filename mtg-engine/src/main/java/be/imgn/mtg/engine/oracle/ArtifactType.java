package be.imgn.mtg.engine.oracle;

/// MTG artifact subtypes (Rule 205.3g).
public enum ArtifactType {
    ATTRACTION("Attraction"),
    BLOOD("Blood"),
    BOBBLEHEAD("Bobblehead"),
    CLUE("Clue"),
    CONTRAPTION("Contraption"),
    EQUIPMENT("Equipment"),
    FOOD("Food"),
    FORTIFICATION("Fortification"),
    GOLD("Gold"),
    INCUBATOR("Incubator"),
    INFINITY("Infinity"),
    JUNK("Junk"),
    LANDER("Lander"),
    MAP("Map"),
    MUTAGEN("Mutagen"),
    POWERSTONE("Powerstone"),
    SPACECRAFT("Spacecraft"),
    STONE("Stone"),
    TREASURE("Treasure"),
    VEHICLE("Vehicle");

    private final String text;

    ArtifactType(String text) {
        this.text = text;
    }

    public String text() {
        return text;
    }

    @Override
    public String toString() {
        return text;
    }
}
