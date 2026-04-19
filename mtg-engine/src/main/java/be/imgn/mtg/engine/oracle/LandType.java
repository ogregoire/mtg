package be.imgn.mtg.engine.oracle;

import java.util.List;

/// MTG land subtypes (Rule 205.3i).
public enum LandType implements Subtype {
    CAVE("Cave"),
    DESERT("Desert"),
    FOREST("Forest"),
    GATE("Gate"),
    ISLAND("Island"),
    LAIR("Lair"),
    LOCUS("Locus", "Loci"),
    MINE("Mine"),
    MOUNTAIN("Mountain"),
    PLAINS("Plains", "Plains"),
    PLANET("Planet"),
    POWER_PLANT("Power-Plant"),
    SPHERE("Sphere"),
    SWAMP("Swamp"),
    TOWER("Tower"),
    TOWN("Town"),
    URZAS("Urza's", "Urza's");

    private final String text;
    private final List<String> texts;

    LandType(String text) {
        this(text, text + "s");
    }

    LandType(String text, String plural) {
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
