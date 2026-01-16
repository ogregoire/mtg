package be.imgn.mtg.engine.game;

/// A player in the game ({@mtg.rule 102}).
///
/// A player is one of the participants in the game. Each player starts with a life total
/// of 20 (in most formats), a hand of seven cards, and a library containing their deck.
///
/// Players can own cards ({@mtg.rule 108.3}), control objects ({@mtg.rule 108.4}), receive
/// priority to cast spells and activate abilities ({@mtg.rule 117}), and make game choices.
/// A player loses when their life total is 0 or less, they attempt to draw from an empty
/// library, or they have 10 or more poison counters ({@mtg.rule 104.3}).
public interface Player {}
