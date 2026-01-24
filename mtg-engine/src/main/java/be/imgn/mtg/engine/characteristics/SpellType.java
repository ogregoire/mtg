package be.imgn.mtg.engine.characteristics;

/// Instant and sorcery subtypes, called spell types ({@mtg.rule 205.3k}).
///
/// Spell types are subtypes that are correlated to the instant and sorcery card types. A spell
/// can have multiple spell types. Examples include Adventure, Arcane, Lesson, and Trap.
public enum SpellType implements Subtype {
    /// The Adventure spell type.
    ADVENTURE,
    /// The Arcane spell type.
    ARCANE,
    /// The Chorus spell type.
    CHORUS,
    /// The Lesson spell type.
    LESSON,
    /// The Trap spell type.
    TRAP
}
