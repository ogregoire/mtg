package be.imgn.mtg.engine.oracle2.domain;

/// A player's relation to the turn structure (CR 502).
public enum PlayerTurnRole {
    /// "the active player" — the player whose turn it is (CR 502.1).
    ACTIVE,
    /// "the nonactive player(s)" — every other player.
    NONACTIVE
}
