package be.imgn.mtg.engine.characteristics;

/// Nonbasic land types ({@mtg.rule 205.3i}).
///
/// These are land types other than the five basic land types. Unlike basic land types, these
/// do not have intrinsic mana abilities. Examples include Cave, Desert, Gate, Lair, and Locus.
public enum NonBasicLandType implements LandType {
    /// The Cave nonbasic land type.
    CAVE("Cave"),
    /// The Desert nonbasic land type.
    DESERT("Desert"),
    /// The Gate nonbasic land type.
    GATE("Gate"),
    /// The Lair nonbasic land type.
    LAIR("Lair"),
    /// The Locus nonbasic land type.
    LOCUS("Locus"),
    /// The Mine nonbasic land type.
    MINE("Mine"),
    /// The Power-Plant nonbasic land type.
    POWER_PLANT("Power-Plant"),
    /// The Sphere nonbasic land type.
    SPHERE("Sphere"),
    /// The Tower nonbasic land type.
    TOWER("Tower"),
    /// The Urza's nonbasic land type.
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
