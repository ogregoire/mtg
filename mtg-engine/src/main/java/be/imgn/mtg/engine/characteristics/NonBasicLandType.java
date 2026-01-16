package be.imgn.mtg.engine.characteristics;

/// Nonbasic land types ({@mtg.rule 205.3i}).
///
/// These are land types other than the five basic land types. Unlike basic land types, these
/// do not have intrinsic mana abilities. Examples include Cave, Desert, Gate, Lair, and Locus.
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
