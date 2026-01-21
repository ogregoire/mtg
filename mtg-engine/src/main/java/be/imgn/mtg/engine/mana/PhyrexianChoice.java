package be.imgn.mtg.engine.mana;

/// A choice for paying a Phyrexian mana symbol ({@mtg.rule 107.4f}).
///
/// Phyrexian symbols can be paid with colored mana or 2 life.
public sealed interface PhyrexianChoice {

    /// Pay with the appropriate colored mana.
    record PayMana() implements PhyrexianChoice {}

    /// Pay 2 life instead of mana.
    record PayLife() implements PhyrexianChoice {}
}
