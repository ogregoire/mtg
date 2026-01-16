package be.imgn.mtg.engine.game;

/// A team of players in a multiplayer game ({@mtg.rule 810}).
///
/// In team formats like Two-Headed Giant, players are organized into teams that share
/// resources and win or lose together. Teams share a starting life total (30 in Two-Headed
/// Giant) and take turns simultaneously.
///
/// In standard two-player games and free-for-all multiplayer, each player is their own team.
/// Team membership affects targeting restrictions, combat, and win/loss conditions.
public interface Team {}
