package be.imgn.mtg.engine.selector;

/// Criteria for matching players relative to a perspective player.
public enum PlayerCriterion {
    /// Any player.
    ANY,
    /// Not the perspective player.
    OPPONENT,
    /// The perspective player.
    YOU
}
