package be.imgn.mtg.engine.oracle;

/// MTG spell subtypes (Rule 205.3k).
public enum SpellType implements Subtype {
    ADVENTURE("Adventure(s)"),
    ARCANE("Arcane(s)"),
    LESSON("Lesson(s)"),
    OMEN("Omen(s)"),
    TRAP("Trap(s)");

    private final String text;

    SpellType(String text) {
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
