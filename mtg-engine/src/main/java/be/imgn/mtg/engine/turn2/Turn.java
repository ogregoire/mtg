package be.imgn.mtg.engine.turn2;

import be.imgn.mtg.engine.game.Player;

/// Represents a turn in the game ({@mtg.rule 500}).
///
/// @param number the turn number (1-indexed)
/// @param activePlayer the player whose turn it is
public record Turn(int number, Player activePlayer) {}
