package be.imgn.mtg.engine.mana;

/// A choice for paying a two-color hybrid mana symbol ({@mtg.rule 107.4e}).
///
/// Two-color hybrid symbols like {W/U} can be paid with either color.
public sealed interface HybridChoice {

    /// Pay with the first color (e.g., {W} in {W/U}).
    record Option1() implements HybridChoice {}

    /// Pay with the second color (e.g., {U} in {W/U}).
    record Option2() implements HybridChoice {}
}
