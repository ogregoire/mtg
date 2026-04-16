package be.imgn.mtg.engine.oracle;

/// MTG spell subtypes (Rule 205.3k).
public enum SpellType {
    ADVENTURE("Adventure"),
    ARCANE("Arcane"),
    LESSON("Lesson"),
    OMEN("Omen"),
    TRAP("Trap");

    private final String text;

    SpellType(String text) {
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
