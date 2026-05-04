package be.imgn.mtg.engine.oracle2.domain.selector;

import be.imgn.mtg.engine.oracle2.domain.PlayerDesignation;

/// Picks a player by a [PlayerDesignation] — "the monarch", "the
/// player with the initiative", "each player with the city's
/// blessing", "ring-tempted player". Player-side counterpart to
/// [ObjectDesignationSelector].
public record PlayerDesignationSelector(PlayerDesignation designation) implements PlayerSelector {}
