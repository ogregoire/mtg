package be.imgn.mtg.engine.mana;

/// A choice for paying a mono-color hybrid mana symbol ({@mtg.rule 107.4e}).
///
/// Mono-color hybrid symbols like {2/W} can be paid with either one colored
/// mana or two mana of any type.
public sealed interface MonoColorHybridChoice {

    /// Pay with one colored mana.
    record PayColor() implements MonoColorHybridChoice {}

    /// Pay with two mana of any type.
    record PayGeneric() implements MonoColorHybridChoice {}
}
