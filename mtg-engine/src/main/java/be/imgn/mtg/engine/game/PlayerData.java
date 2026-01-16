package be.imgn.mtg.engine.game;

/// Initial data for a player joining a game.
///
/// This record contains the information needed to set up a player at the start
/// of a game, including their identifier and team assignment.
///
/// @param id the unique identifier for this player
/// @param team the team this player belongs to
public record PlayerData(PlayerId id, Team team) {
    // TODO Add Deck
}
