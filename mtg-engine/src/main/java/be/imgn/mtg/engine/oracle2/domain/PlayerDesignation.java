package be.imgn.mtg.engine.oracle2.domain;

/// A player-level designation — game state attached to a specific
/// player that isn't part of the player's life total, hand, or
/// counters. Each designation is held by zero or one player at a
/// time (with a few exceptions noted below) and persists across
/// turns until a rule or effect transfers it.
///
/// Distinct from object-level [ObjectDesignation] (commander, Ring-bearer):
/// those live on cards or permanents.
public enum PlayerDesignation {
    /// The monarch (CR 716). One player at a time; passes when that
    /// player takes combat damage from a creature.
    MONARCH,
    /// The initiative (CR 720). One player at a time; held by the
    /// player who most recently took the initiative.
    INITIATIVE,
    /// City's blessing (CR 702.131c). Any number of players may
    /// have it simultaneously; once gained, it isn't lost.
    CITY_BLESSING,
    /// Ring-tempted: the player has had the Ring tempt them at
    /// least once (CR 701.54). Tracked via the emblem named The
    /// Ring; this designation reflects whether that emblem exists.
    RING_TEMPTED
}
