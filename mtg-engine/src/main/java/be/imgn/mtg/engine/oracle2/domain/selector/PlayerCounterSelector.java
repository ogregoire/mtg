package be.imgn.mtg.engine.oracle.domain2.selector;

/// Selects a player by counters on them (CR 122.1) — "player with
/// three or more poison counters", "opponent with at least one
/// energy counter", etc. Player-side counterpart to
/// [ObjectCounterSelector]. Player counter types are partitioned
/// from object ones (poison, energy, rad, experience, ticket — all
/// player-only).
public non-sealed interface PlayerCounterSelector extends PlayerSelector {}
