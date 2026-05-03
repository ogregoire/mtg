package be.imgn.mtg.engine.oracle.domain2;

/// A player's relation to the turn structure (CR 502).
public enum PlayerTurnRole {
    /// "the active player" — the player whose turn it is (CR 502.1).
    ACTIVE,
    /// "the nonactive player(s)" — every other player.
    NONACTIVE
}
