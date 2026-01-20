package be.imgn.mtg.engine.result;

/// Reasons why a player loses the game ({@mtg.rule 104.3}).
///
/// A player can lose the game for various reasons including state-based actions,
/// game effects, or by their own choice (concession).
public enum LossReason {
    /// The player chose to concede ({@mtg.rule 104.3a}).
    CONCESSION,

    /// The player's life total became 0 or less ({@mtg.rule 104.3b}, {@mtg.rule 704.5a}).
    ZERO_LIFE,

    /// The player attempted to draw a card from an empty library ({@mtg.rule 104.3c}, {@mtg.rule 704.5b}).
    EMPTY_LIBRARY_DRAW,

    /// The player has 10 or more poison counters ({@mtg.rule 104.3d}, {@mtg.rule 704.5c}).
    POISON,

    /// A card or effect caused the player to lose ({@mtg.rule 104.3e}).
    EFFECT,

    /// The player took 21 or more combat damage from a single commander ({@mtg.rule 704.5d}).
    COMMANDER_DAMAGE
}
