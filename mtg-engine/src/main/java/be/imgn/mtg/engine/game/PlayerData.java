package be.imgn.mtg.engine.game;

import org.jspecify.annotations.Nullable;

import be.imgn.mtg.engine.card.DeckDefinition;

/// Initial data for a player joining a game ({@mtg.rule 103}).
///
/// Contains the information needed to set up a [Player] during game initialization. This
/// includes their identifier, team assignment, and optionally their deck. Before the
/// game begins, each player's deck becomes their library ({@mtg.rule 103.2}).
///
/// @param id   the unique identifier for this player
/// @param team the team this player belongs to
/// @param deck the deck this player will use, or null if not yet assigned
public record PlayerData(PlayerId id, Team team, @Nullable DeckDefinition deck) {

    /// Creates player data without a deck.
    ///
    /// @param id   the unique identifier for this player
    /// @param team the team this player belongs to
    public PlayerData(PlayerId id, Team team) {
        this(id, team, null);
    }
}
