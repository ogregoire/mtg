package be.imgn.mtg.engine.ability.internal.parser.reference;

/// Restricts selection to objects controlled by a specific player.
///
/// @param player the player whose objects are selected
public record ControllerClause(PlayerReference player) {}
