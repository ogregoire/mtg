package be.imgn.mtg.engine.oracle.domain;

import java.util.List;

/// One option in an [Effect.AddMana] clause. Oracle text can express
/// either a fixed set of mana symbols (`Add {G}{G}`) or a variable number of
/// a single color (`Add X mana of any one color`). The five alternative
/// options of "any one color" are modelled as five [Repeated] options,
/// one per basic color.
public sealed interface ManaOption {

    /// A concrete list of mana symbols added as a group (e.g., `{G}{G}`).
    record Fixed(List<ManaSymbol> symbols) implements ManaOption {}

    /// `amount` copies of a single `color` — used for "N mana of [color]"
    /// and "X mana of any one color" (one `Repeated` per basic color).
    record Repeated(Amount count, ManaSymbol color) implements ManaOption {}

    /// `count` copies of the color previously named in the same
    /// resolution — the oracle's "that color" back-reference (Meteor
    /// Crater: "Choose a color of a permanent you control. Add one
    /// mana of that color."). The binding source is typically a
    /// preceding [Effect.ChooseColor], but can also come from a
    /// Reveal or trigger context, so this variant only says "of that
    /// color" without asserting its origin.
    record OfThatColor(Amount count) implements ManaOption {}

    /// `count` mana total, each chosen independently from `palette`
    /// (Manamorphose: "Add two mana in any combination of colors";
    /// Orcish Lumberjack: "Add three mana in any combination of {R}
    /// and/or {G}"). Distinct from a list of [Repeated] alternatives:
    /// here the player may mix symbols across the palette rather than
    /// pick one all-of-the-same-color menu entry.
    record Combination(Amount count, List<ManaSymbol> palette) implements ManaOption {}

    /// `count` copies of a color/type that `source` could produce —
    /// dynamic mana palette derived from the source permanent's own
    /// mana ability (Squandered Resources: "Add one mana of any type
    /// the sacrificed land could produce."). Distinct from [Repeated]
    /// × 5 basic colors: the palette is whatever that specific land
    /// actually taps for, not blindly WUBRG.
    record ProducedBy(Amount count, Subject source) implements ManaOption {}
}
