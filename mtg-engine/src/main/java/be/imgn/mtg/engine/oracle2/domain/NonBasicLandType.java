package be.imgn.mtg.engine.oracle2.domain;

/// Non-basic land subtypes ({@mtg.rule 205.3i}) — every printed land
/// subtype other than the five basic ones in [BasicLandType]. Lands
/// with only these subtypes are not basic lands.
public enum NonBasicLandType implements LandType {
    CAVE("Cave(s)"),
    DESERT("Desert(s)"),
    GATE("Gate(s)"),
    LAIR("Lair(s)"),
    LOCUS("[Locus|Loci]"),
    MINE("Mine(s)"),
    PLANET("Planet(s)"),
    POWER_PLANT("Power-Plant(s)"),
    SPHERE("Sphere(s)"),
    TOWER("Tower(s)"),
    TOWN("Town(s)"),
    URZAS("Urza's");

    private final String text;

    NonBasicLandType(String text) {
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
