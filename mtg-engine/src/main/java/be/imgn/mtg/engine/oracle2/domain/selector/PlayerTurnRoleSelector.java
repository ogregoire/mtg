package be.imgn.mtg.engine.oracle.domain2.selector;

import be.imgn.mtg.engine.oracle.domain2.PlayerTurnRole;

/// Picks a player by their [PlayerTurnRole] — "the active player",
/// "each nonactive player". Independent axis from
/// [PlayerRelationSelector] (relation to "you") and
/// [CombatRoleSelector] (combat side); combinations stack via clauses.
public record PlayerTurnRoleSelector(PlayerTurnRole role) implements PlayerSelector {}
