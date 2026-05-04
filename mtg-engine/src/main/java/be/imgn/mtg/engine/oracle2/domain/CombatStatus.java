package be.imgn.mtg.engine.oracle2.domain;

/// Combat status of an object — the role a creature or other
/// permanent plays during the combat phase (CR 506–509). A combat
/// status sits alongside status (CR 110.5) and characteristics
/// (CR 109.3) rather than within them.
public enum CombatStatus {
    /// "attacking creature" — declared as an attacker (CR 508).
    ATTACKING,
    /// "blocking creature" — declared as a blocker (CR 509).
    BLOCKING,
    /// "blocked creature" — an attacker that has been blocked.
    BLOCKED,
    /// "unblocked attacking creature" — an attacker that wasn't
    /// blocked.
    UNBLOCKED,
    /// "attacked creature/planeswalker/battle" — a planeswalker or
    /// battle being attacked.
    ATTACKED
}
