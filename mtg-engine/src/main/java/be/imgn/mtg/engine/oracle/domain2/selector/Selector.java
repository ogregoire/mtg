package be.imgn.mtg.engine.oracle.domain2.selector;

/// Root of the selector hierarchy: anything an oracle-text phrase can
/// pick out. Splits into [PlayerSelector] for player slots,
/// [ObjectSelector] for objects (cards, permanents, spells, …),
/// [Target] which marks a selector as a target of a spell or ability
/// (CR 115), and [QuantifierSelector] which adds a count axis on top
/// of any of the above ("two creatures", "each opponent", …).
public sealed interface Selector permits PlayerSelector, ObjectSelector, Target, QuantifierSelector {}
