package be.imgn.mtg.engine.oracle;

import java.util.List;

/// MTG artifact subtypes (Rule 205.3g).
public enum ArtifactType implements Subtype {
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
    INFINITY("Infinity", "Infinity"),
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
    private final List<String> texts;

    ArtifactType(String text) {
        this(text, text + "s");
    }

    ArtifactType(String text, String plural) {
        this.text = text;
        this.texts = plural.equals(text) ? List.of(text) : List.of(text, plural);
    }

    public String text() {
        return text;
    }

    @Override
    public List<String> texts() {
        return texts;
    }

    @Override
    public String toString() {
        return text;
    }
}
