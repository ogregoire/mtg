package be.imgn.mtg.engine.oracle2.domain.selector;

/// Selects an object by its rules text (CR 207) — rare; used by
/// effects that key off literal rules-text content.
public non-sealed interface RulesTextSelector extends CharacteristicSelector {}
