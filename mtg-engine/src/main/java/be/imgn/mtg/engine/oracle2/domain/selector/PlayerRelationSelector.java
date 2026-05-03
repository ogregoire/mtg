package be.imgn.mtg.engine.oracle.domain2.selector;

import be.imgn.mtg.engine.oracle.domain2.PlayerRelation;

/// Picks a player by their [PlayerRelation] to the controller of the
/// resolving spell or ability — "you", "an opponent", "a teammate".
public record PlayerRelationSelector(PlayerRelation relation) implements PlayerSelector {}
