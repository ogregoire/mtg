package be.imgn.mtg.engine.oracle.domain2.selector;

import be.imgn.mtg.engine.oracle.domain2.CombatRole;

/// Picks a player by their [CombatRole] (CR 506–509) — "the
/// attacking player", "the defending player", "if the defending
/// player is you".
public record CombatRoleSelector(CombatRole role) implements PlayerSelector {}
