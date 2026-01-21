package be.imgn.mtg.engine.mana;

/// An assignment of mana to pay for a mana symbol.
///
/// Used during payment to track which mana from the pool pays for which symbol in the cost.
///
/// @param symbol the mana symbol being paid
/// @param mana the mana from the pool used to pay (null for life payment)
public record ManaAssignment(ManaSymbol symbol, Mana mana) {}
