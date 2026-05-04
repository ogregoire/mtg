package be.imgn.mtg.engine.oracle2.domain.selector;

import be.imgn.mtg.engine.oracle2.domain.PlayerTurnRole;

/// Picks a player by their [PlayerTurnRole] — "the active player",
/// "each nonactive player". Independent axis from
/// [PlayerRelationSelector] (relation to "you") and
/// [CombatRoleSelector] (combat side); combinations stack via clauses.
public record PlayerTurnRoleSelector(PlayerTurnRole role) implements PlayerSelector {}
