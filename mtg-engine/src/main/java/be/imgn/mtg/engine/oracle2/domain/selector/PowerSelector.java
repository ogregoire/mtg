package be.imgn.mtg.engine.oracle2.domain.selector;

/// Selects a creature by its power (CR 208) — "creature with power
/// 3 or greater", "creature with power equal to its toughness", etc.
public non-sealed interface PowerSelector extends CharacteristicSelector {}
