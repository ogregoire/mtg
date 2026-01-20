package be.imgn.mtg.engine.result;

import java.util.List;

import be.imgn.mtg.engine.game.Player;

/// The result of a completed game ({@mtg.rule 104}).
///
/// A game can end with a single winner, multiple winners (team formats),
/// a draw, or a restart (rare effects like Karn Liberated).
public sealed interface GameResult {

    /// A single player won the game.
    ///
    /// @param winner the winning player
    /// @param condition how the player won
    record Winner(Player winner, WinCondition condition) implements GameResult {}

    /// Multiple players won the game (team formats).
    ///
    /// @param winners the winning players
    /// @param condition how the team won
    record Winners(List<Player> winners, WinCondition condition) implements GameResult {}

    /// The game ended in a draw.
    ///
    /// @param condition why the game was a draw
    record Draw(DrawCondition condition) implements GameResult {}

    /// The game should be restarted ({@mtg.rule 104.6g}).
    ///
    /// Some effects like Karn Liberated's ultimate cause the game to restart.
    /// @param cause a description of what caused the restart
    record Restart(String cause) implements GameResult {}
}
