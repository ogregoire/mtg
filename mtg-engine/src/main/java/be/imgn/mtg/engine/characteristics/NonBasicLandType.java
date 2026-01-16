package be.imgn.mtg.engine.characteristics;

/// Non-basic land types (rule 205.3i).
public enum NonBasicLandType implements LandType {
    CAVE("Cave"),
    DESERT("Desert"),
    GATE("Gate"),
    LAIR("Lair"),
    LOCUS("Locus"),
    MINE("Mine"),
    POWER_PLANT("Power-Plant"),
    SPHERE("Sphere"),
    TOWER("Tower"),
    URZAS("Urza's");

    private final String text;

    NonBasicLandType(String text) {
        this.text = text;
    }

    @Override
    public String text() {
        return text;
    }
}
