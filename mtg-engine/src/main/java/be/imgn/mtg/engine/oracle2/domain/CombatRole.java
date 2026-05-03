package be.imgn.mtg.engine.oracle.domain2;

/// Combat role of a player — the side a player is on during combat
/// (CR 506–509). Each combat involves exactly one attacking player
/// and one or more defending players (CR 506.2).
public enum CombatRole {
    /// "the attacking player" — the active player whose creatures
    /// are attacking (CR 506.2).
    ATTACKING,
    /// "the defending player" — a player whose life total or
    /// permanents are being attacked (CR 506.2).
    DEFENDING
}
