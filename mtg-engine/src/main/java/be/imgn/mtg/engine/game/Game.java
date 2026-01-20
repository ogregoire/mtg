package be.imgn.mtg.engine.game;

import java.util.List;

/// A Magic: The Gathering game ({@mtg.rule 100}).
///
/// A game consists of two or more players who use their decks to play against each other.
/// The game tracks the game state including zones, turn structure, the stack, priority
/// passing, and state-based actions ({@mtg.rule 704}).
///
/// A game begins when all players have shuffled and drawn their opening hands. Players
/// take turns in order ({@mtg.rule 103}), and the game ends when a player wins, loses,
/// or the game is a draw ({@mtg.rule 104}).
public interface Game {

    /// Returns all players in this game.
    ///
    /// @return an unmodifiable list of players in turn order
    List<Player> players();
}
