package be.imgn.mtg.engine.mana;

/// A choice for paying a hybrid mana symbol ({@mtg.rule 107.4e}).
///
/// Hybrid symbols can be paid with either of two options.
public sealed interface HybridChoice {

    /// Pay with the first option (e.g., {W} in {W/U}).
    record Option1() implements HybridChoice {}

    /// Pay with the second option (e.g., {U} in {W/U}).
    /// For mono-hybrid, this means paying 2 generic mana.
    /// For colorless-hybrid, this means paying colorless.
    record Option2() implements HybridChoice {}
}
