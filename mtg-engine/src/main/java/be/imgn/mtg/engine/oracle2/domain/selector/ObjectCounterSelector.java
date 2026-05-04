package be.imgn.mtg.engine.oracle2.domain.selector;

/// Selects an object by counters on it (CR 122.1) — "creature with
/// a +1/+1 counter on it", "permanent with three or more loyalty
/// counters", etc. Object-side counterpart to [PlayerCounterSelector].
public non-sealed interface ObjectCounterSelector extends ObjectPropertySelector {}
