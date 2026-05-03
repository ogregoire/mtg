package be.imgn.mtg.engine.oracle.domain2;

/// The five basic land subtypes ({@mtg.rule 305.6}). A land with one
/// of these subtypes is a basic land iff it also has the `Basic`
/// supertype.
public enum BasicLandType implements LandType {
    PLAINS("Plains"),
    ISLAND("Island(s)"),
    SWAMP("Swamp(s)"),
    MOUNTAIN("Mountain(s)"),
    FOREST("Forest(s)");

    private final String text;

    BasicLandType(String text) {
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
