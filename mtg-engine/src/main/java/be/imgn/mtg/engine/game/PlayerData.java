package be.imgn.mtg.engine.game;

/// Initial data for a player joining a game ({@mtg.rule 103}).
///
/// Contains the information needed to set up a [Player] during game initialization. This
/// includes their identifier, team assignment, and (eventually) their deck. Before the
/// game begins, each player's deck becomes their library ({@mtg.rule 103.2}).
///
/// @param id the unique identifier for this player
/// @param team the team this player belongs to
public record PlayerData(PlayerId id, Team team) {
    // TODO Add Deck
}
