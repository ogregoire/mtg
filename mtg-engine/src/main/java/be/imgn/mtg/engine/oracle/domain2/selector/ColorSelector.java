package be.imgn.mtg.engine.oracle.domain2.selector;

/// Selects an object by its color (CR 202, 105) — "red creature",
/// "nonblack permanent", "multicolored spell", etc.
public non-sealed interface ColorSelector extends CharacteristicSelector {}
