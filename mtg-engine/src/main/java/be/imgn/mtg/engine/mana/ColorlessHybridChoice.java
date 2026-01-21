package be.imgn.mtg.engine.mana;

/// A choice for paying a colorless hybrid mana symbol ({@mtg.rule 107.4e}).
///
/// Colorless hybrid symbols like {C/W} can be paid with either colorless
/// mana or one colored mana.
public sealed interface ColorlessHybridChoice {

    /// Pay with colored mana.
    record PayColor() implements ColorlessHybridChoice {}

    /// Pay with colorless mana.
    record PayColorless() implements ColorlessHybridChoice {}
}
