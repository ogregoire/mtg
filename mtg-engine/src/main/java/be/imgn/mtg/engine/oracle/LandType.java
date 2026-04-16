package be.imgn.mtg.engine.oracle;

/// MTG land subtypes (Rule 205.3i).
public enum LandType {
    CAVE("Cave"),
    DESERT("Desert"),
    FOREST("Forest"),
    GATE("Gate"),
    ISLAND("Island"),
    LAIR("Lair"),
    LOCUS("Locus"),
    MINE("Mine"),
    MOUNTAIN("Mountain"),
    PLAINS("Plains"),
    PLANET("Planet"),
    POWER_PLANT("Power-Plant"),
    SPHERE("Sphere"),
    SWAMP("Swamp"),
    TOWER("Tower"),
    TOWN("Town"),
    URZAS("Urza's");

    private final String text;

    LandType(String text) {
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
