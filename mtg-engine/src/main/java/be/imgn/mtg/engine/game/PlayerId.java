package be.imgn.mtg.engine.game;

/// A unique identifier for a player, provided by the game's user.
///
/// PlayerId is an opaque interface that allows users of the engine to attach any
/// information they need to identify players. The engine does not interpret or use
/// the contents of PlayerId - it simply stores and returns it.
///
/// Example implementations:
/// ```java
/// // Simple string-based ID
/// record SimplePlayerId(String name) implements PlayerId {}
///
/// // Rich player data with avatar
/// record RichPlayerId(String name, String avatarUrl, int rating) implements PlayerId {}
///
/// // Database-backed ID
/// record DbPlayerId(long id) implements PlayerId {}
/// ```
///
/// The engine uses [Player] for all game logic (ownership, control, zones).
/// PlayerId is only for external identification and display purposes.
///
/// @see Player
/// @see PlayerData
public interface PlayerId {}
