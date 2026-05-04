package be.imgn.mtg.engine.oracle2.domain.selector;

import be.imgn.mtg.engine.oracle2.domain.CombatRole;

/// Picks a player by their [CombatRole] (CR 506–509) — "the
/// attacking player", "the defending player", "if the defending
/// player is you".
public record CombatRoleSelector(CombatRole role) implements PlayerSelector {}
