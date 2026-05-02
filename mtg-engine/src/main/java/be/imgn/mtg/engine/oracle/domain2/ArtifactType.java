package be.imgn.mtg.engine.oracle.domain2;

/// MTG artifact subtypes (Rule 205.3g).
public enum ArtifactType implements Subtype {
    ATTRACTION("Attraction(s)"),
    BLOOD("Blood(s)"),
    BOBBLEHEAD("Bobblehead(s)"),
    BOOK("Book(s)"),
    CLUE("Clue(s)"),
    CONTRAPTION("Contraption(s)"),
    EQUIPMENT("Equipment(s)"),
    FOOD("Food(s)"),
    FORTIFICATION("Fortification(s)"),
    GOLD("Gold(s)"),
    INCUBATOR("Incubator(s)"),
    INFINITY("Infinity"),
    JUNK("Junk(s)"),
    LANDER("Lander(s)"),
    MAP("Map(s)"),
    MUTAGEN("Mutagen(s)"),
    POWERSTONE("Powerstone(s)"),
    SPACECRAFT("Spacecraft(s)"),
    STONE("Stone(s)"),
    TREASURE("Treasure(s)"),
    VEHICLE("Vehicle(s)");

    private final String text;

    ArtifactType(String text) {
        this.text = text;
    }

    @Override
    public String text() {
        return text;
    }

    @Override
    public String toString() {
        return texts().getFirst();
    }
}
