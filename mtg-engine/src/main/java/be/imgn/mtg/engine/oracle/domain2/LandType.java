package be.imgn.mtg.engine.oracle.domain2;

/// MTG land subtypes (Rule 205.3i).
public enum LandType implements Subtype {
    CAVE("Cave(s)"),
    DESERT("Desert(s)"),
    FOREST("Forest(s)"),
    GATE("Gate(s)"),
    ISLAND("Island(s)"),
    LAIR("Lair(s)"),
    LOCUS("[Locus|Loci]"),
    MINE("Mine(s)"),
    MOUNTAIN("Mountain(s)"),
    PLAINS("Plains"),
    PLANET("Planet(s)"),
    POWER_PLANT("Power-Plant(s)"),
    SPHERE("Sphere(s)"),
    SWAMP("Swamp(s)"),
    TOWER("Tower(s)"),
    TOWN("Town(s)"),
    URZAS("Urza's");

    private final String text;

    LandType(String text) {
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
