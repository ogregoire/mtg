package be.imgn.mtg.engine.oracle2.domain.selector;

import be.imgn.mtg.engine.oracle2.domain.PlayerRelation;

/// Picks a player by their [PlayerRelation] to the controller of the
/// resolving spell or ability — "you", "an opponent", "a teammate".
public record PlayerRelationSelector(PlayerRelation relation) implements PlayerSelector {
    /// Canonical instance for the implicit "you" subject — shared by
    /// every verb-first imperative parser that fills its subject with
    /// "you" (Draw, Scry, …).
    public static final PlayerRelationSelector YOU = new PlayerRelationSelector(PlayerRelation.YOU);
}
