package be.imgn.mtg.engine.oracle;

import java.util.List;

/// One option in an {@link Effect.AddMana} clause. Oracle text can express
/// either a fixed set of mana symbols (`Add {G}{G}`) or a variable number of
/// a single color (`Add X mana of any one color`). The five alternative
/// options of "any one color" are modelled as five {@link Repeated} options,
/// one per basic color.
public sealed interface ManaOption {

    /// A concrete list of mana symbols added as a group (e.g., `{G}{G}`).
    record Fixed(List<ManaSymbol> symbols) implements ManaOption {}

    /// `amount` copies of a single {@code color} — used for "N mana of [color]"
    /// and "X mana of any one color" (one {@code Repeated} per basic color).
    record Repeated(Amount count, ManaSymbol color) implements ManaOption {}
}
