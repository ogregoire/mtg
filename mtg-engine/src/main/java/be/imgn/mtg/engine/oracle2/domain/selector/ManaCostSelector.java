package be.imgn.mtg.engine.oracle.domain2.selector;

/// Selects an object by its mana cost (CR 202) — "spell with mana
/// value 3", "creature with converted mana cost X", etc.
public non-sealed interface ManaCostSelector extends CharacteristicSelector {}
