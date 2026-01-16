package be.imgn.mtg.engine.characteristics;

/// Spell subtypes for instants and sorceries (rule 205.3k).
public enum SpellType implements Subtype {
    ADVENTURE("Adventure"),
    ARCANE("Arcane"),
    CHORUS("Chorus"),
    LESSON("Lesson"),
    TRAP("Trap");

    private final String text;

    SpellType(String text) {
        this.text = text;
    }

    @Override
    public String text() {
        return text;
    }
}
