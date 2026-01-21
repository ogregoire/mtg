package be.imgn.mtg.engine.mana;

/// A choice for paying a hybrid Phyrexian mana symbol ({@mtg.rule 107.4g}).
///
/// Hybrid Phyrexian symbols like {W/U/P} can be paid with either color or 2 life.
public sealed interface HybridPhyrexianChoice {

    /// Pay with the first color option (e.g., {W} in {W/U/P}).
    record PayColor1() implements HybridPhyrexianChoice {}

    /// Pay with the second color option (e.g., {U} in {W/U/P}).
    record PayColor2() implements HybridPhyrexianChoice {}

    /// Pay 2 life instead of mana.
    record PayLife() implements HybridPhyrexianChoice {}
}
