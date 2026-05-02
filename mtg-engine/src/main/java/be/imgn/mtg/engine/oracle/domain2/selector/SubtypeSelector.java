package be.imgn.mtg.engine.oracle.domain2.selector;

/// Selects an object by its subtype (CR 205.3) — creature types
/// ("Goblin"), enchantment types ("Aura"), land types ("Forest"),
/// spell types ("Arcane"), etc.
public non-sealed interface SubtypeSelector extends CharacteristicSelector {}
