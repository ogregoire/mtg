package be.imgn.mtg.engine.game;

import be.imgn.mtg.engine.event.Event;

/// Event fired when a player leaves the game ({@mtg.rule 104.5}).
///
/// A player leaves the game when they win, lose, the game is a draw for them,
/// or they concede. Once a player has left the game, they no longer participate
/// in turns, priority, or any game actions.
///
/// @param player the player who left the game
/// @param reason the reason the player left
public record PlayerLeftEvent(Player player, Reason reason) implements Event {

    /// The reason a player left the game.
    public enum Reason {
        /// The player won the game ({@mtg.rule 104.1}).
        WON,

        /// The player lost the game ({@mtg.rule 104.3}).
        LOST,

        /// The game was a draw for this player ({@mtg.rule 104.4}).
        DRAW,

        /// The player conceded ({@mtg.rule 104.3a}).
        CONCEDED
    }
}
