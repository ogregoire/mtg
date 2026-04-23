package be.imgn.mtg.engine.oracle.domain;

/// Format-specific designations that appear in oracle text as
/// type-slot words without being MTG subtypes. Commander-format
/// cards use "commander" (rule 903) to name the card chosen to lead
/// a deck; the word behaves type-like in sentences such as Witch's
/// Clinic's "target commander" but isn't a subtype per rule 205.3.
public enum Role {
    COMMANDER;

    /// Canonical lowercase oracle spelling.
    public String text() {
        return name().toLowerCase();
    }
}
