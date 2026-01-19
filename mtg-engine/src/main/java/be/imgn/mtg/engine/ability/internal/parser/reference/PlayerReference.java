package be.imgn.mtg.engine.ability.internal.parser.reference;

/// References to players in oracle text.
public enum PlayerReference {
    /// The controller of the source (you).
    YOU,
    /// An opponent of the controller.
    OPPONENT,
    /// A previously referenced player (that player).
    THAT_PLAYER,
    /// A targeted player.
    TARGET_PLAYER,
    /// Each player in the game.
    EACH_PLAYER,
    /// Each opponent.
    EACH_OPPONENT
}
