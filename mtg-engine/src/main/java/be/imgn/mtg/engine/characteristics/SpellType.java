package be.imgn.mtg.engine.characteristics;

/// Instant and sorcery subtypes, called spell types ({@mtg.rule 205.3k}).
///
/// Spell types are subtypes that are correlated to the instant and sorcery card types. A spell
/// can have multiple spell types. Examples include Adventure, Arcane, Lesson, and Trap.
public enum SpellType implements Subtype {
    /// The Adventure spell type.
    ADVENTURE("Adventure"),
    /// The Arcane spell type.
    ARCANE("Arcane"),
    /// The Chorus spell type.
    CHORUS("Chorus"),
    /// The Lesson spell type.
    LESSON("Lesson"),
    /// The Trap spell type.
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
