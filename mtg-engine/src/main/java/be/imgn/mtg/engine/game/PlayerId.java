package be.imgn.mtg.engine.game;

/// A unique identifier for a player in the game ({@mtg.rule 102}).
///
/// Player IDs provide a stable reference to a player that persists across game state
/// serialization and external system integration. Each player in a game has a unique ID
/// assigned before the game begins.
public interface PlayerId {}
