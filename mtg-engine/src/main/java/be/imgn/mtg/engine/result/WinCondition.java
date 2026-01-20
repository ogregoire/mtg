package be.imgn.mtg.engine.result;

import be.imgn.mtg.engine.game.Player;

/// Conditions under which a player or team wins the game ({@mtg.rule 104.2}).
///
/// Win conditions describe how a game was won - either by being the last player
/// standing, through a card effect, or by team victory.
public sealed interface WinCondition {

    /// The player won because all other players lost ({@mtg.rule 104.2a}).
    ///
    /// @param winner the player who won
    record LastPlayerStanding(Player winner) implements WinCondition {}

    /// The player won due to a card or effect ({@mtg.rule 104.2b}).
    ///
    /// @param winner the player who won
    /// @param cause a description of the winning effect (e.g., "Laboratory Maniac")
    record EffectWin(Player winner, String cause) implements WinCondition {}

    /// A team won the game (team formats only).
    ///
    /// @param winningTeamId the identifier of the winning team
    record TeamWin(Object winningTeamId) implements WinCondition {}
}
