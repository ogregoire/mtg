package be.imgn.mtg.engine.oracle.domain;

/// Format-specific designations that appear in oracle text as
/// type-slot words without being MTG subtypes. Commander-format
/// cards use "commander" (rule 903) to name the card chosen to lead
/// a deck; the word behaves type-like in sentences such as Witch's
/// Clinic's "target commander" but isn't a subtype per rule 205.3.
/// Ring-bearer cards use "Ring-bearer" (rule 716.1a) to name the
/// designated permanent bearing the One Ring's temptation.
public enum Role {
    COMMANDER,
    /// The Ring-bearer designation (rule 716.1a).
    RING_BEARER;

    /// Canonical lowercase oracle spelling.
    public String text() {
        return switch (this) {
            case COMMANDER -> "commander";
            case RING_BEARER -> "Ring-bearer";
        };
    }
}
