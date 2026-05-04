package be.imgn.mtg.engine.oracle2.domain;

/// A player's relation to the controller of the resolving spell or
/// ability ("you", per CR 109.5). Values are not all mutually
/// exclusive — [#TEAM] covers [#YOU] and the player's [#TEAMMATE]s.
///
/// "Another player" / "other players" are *relational* — picked by
/// `OtherPlayerSelector(than)` rather than as an enum value here.
public enum PlayerRelation {
    /// "you" — the controller (CR 109.5).
    YOU,
    /// "an opponent", "your opponents" — CR 102.6.
    OPPONENT,
    /// "a teammate" — a player on your team, NOT including you
    /// (CR 102.4, 802). Use [#TEAM] for the inclusive form.
    TEAMMATE,
    /// "your team" (CR 802 — Two-Headed Giant and similar). The full
    /// set: you and your teammates. For "your teammate" / "each of
    /// your teammates" (excluding you), use [#TEAMMATE] instead.
    TEAM
}
