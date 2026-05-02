package be.imgn.mtg.engine.oracle.domain2.selector;

/// Selects a permanent by its status (CR 110.5) — tapped/untapped,
/// flipped/unflipped, face up/face down, phased in/phased out.
/// Status isn't a characteristic (CR 110.5a).
public non-sealed interface StatusSelector extends ObjectPropertySelector {}
