package be.imgn.mtg.engine.oracle2.domain.selector;

/// Selects an object by its color indicator (CR 204) — the dot on
/// cards whose color isn't expressible via the mana cost.
public non-sealed interface ColorIndicatorSelector extends CharacteristicSelector {}
