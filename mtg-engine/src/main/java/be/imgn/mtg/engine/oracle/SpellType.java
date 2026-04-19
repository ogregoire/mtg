package be.imgn.mtg.engine.oracle;

import java.util.List;

/// MTG spell subtypes (Rule 205.3k).
public enum SpellType implements Subtype {
    ADVENTURE("Adventure"),
    ARCANE("Arcane"),
    LESSON("Lesson"),
    OMEN("Omen"),
    TRAP("Trap");

    private final String text;
    private final List<String> texts;

    SpellType(String text) {
        this(text, text + "s");
    }

    SpellType(String text, String plural) {
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
