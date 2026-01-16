package be.imgn.mtg.engine.game;

import java.util.List;

import be.imgn.mtg.engine.format.Format;

/// Factory for creating [Game] instances ({@mtg.rule 103}).
///
/// Creates and initializes new games according to the starting procedures defined in the
/// rules. This includes determining starting player, shuffling libraries, and drawing
/// opening hands.
///
/// Obtain an instance via Guice injection after installing the GameModule.
///
/// Example usage:
/// ```java
/// var injector = Guice.createInjector(new GameModule());
/// var factory = injector.getInstance(GameFactory.class);
/// var game = factory.createGame(format, playersData);
/// ```
public interface GameFactory {

    /// Creates a new game with the given format and players.
    ///
    /// @param format the game format defining rules and configuration
    /// @param players the list of players joining the game
    /// @return a new game instance
    Game createGame(Format format, List<PlayerData> players);
}
