package be.imgn.mtg.engine.characteristics;

/// Basic land types (rule 305.6).
public enum BasicLandType implements LandType {
    PLAINS("Plains"),
    ISLAND("Island"),
    SWAMP("Swamp"),
    MOUNTAIN("Mountain"),
    FOREST("Forest");

    private final String text;

    BasicLandType(String text) {
        this.text = text;
    }

    @Override
    public String text() {
        return text;
    }
}
