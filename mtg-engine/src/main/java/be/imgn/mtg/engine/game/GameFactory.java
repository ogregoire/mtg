package be.imgn.mtg.engine.game;

import java.util.List;

import be.imgn.mtg.engine.format.Format;

/// Factory for creating [Game] instances.
///
/// This is the main entry point for creating games. Obtain an instance via
/// Guice injection after installing the GameModule.
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
