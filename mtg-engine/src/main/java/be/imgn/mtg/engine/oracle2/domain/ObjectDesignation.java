package be.imgn.mtg.engine.oracle2.domain;

/// A designation that's not part of an object's characteristics
/// (rule 109.3) but that oracle text refers to type-like — e.g.,
/// "target commander". Designations sit alongside characteristics
/// rather than within them.
///
/// {@link #COMMANDER} is a card-level attribute that travels with
/// the card across all zones (rule 903.3). {@link #RING_BEARER} is
/// a permanent-level designation lost when leaving the battlefield
/// (rule 701.54b). Despite the different lifetimes, both are
/// "designations" in the rules' sense.
public enum ObjectDesignation {
    /// Designates a card as its deck's commander. {@mtg.rule 903.3}.
    COMMANDER,
    /// The Ring-bearer designation a creature can have. {@mtg.rule 701.54b}.
    RING_BEARER;

    /// Canonical lowercase oracle spelling.
    public String text() {
        return switch (this) {
            case COMMANDER -> "commander";
            case RING_BEARER -> "Ring-bearer";
        };
    }
}
