package be.imgn.mtg.engine.game;

/// A unique identifier for a player in the game.
///
/// Player IDs are used to reference players without holding direct references
/// to [Player] objects, enabling serialization and external player management.
public interface PlayerId {}
