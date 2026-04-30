package be.imgn.mtg.engine.oracle.domain;

import java.util.List;

/// The mana payload of an [AddManaEffect]. Replaces the prior
/// `List<ManaOption>` shape, which conflated alternation, repetition,
/// and compound addition by relying on list semantics.
///
/// Organising principle: anything written *literally* in mana symbols
/// (`{G}`, `{2}{B}`, `{W}{W}`) lands as [Exact]. Anything *described*
/// in words ("three mana of any one color", "an amount of {G} equal
/// to creature's power") lands in a typed described variant.
public sealed interface Mana {

    // ── Literal mana ─────────────────────────────────────────────────

    /// Literal sequence of mana symbols, exactly as written on the
    /// card. Covers every shape that's spelled out in `{...}`
    /// notation with no described count: `{G}` (Exact([{G}])),
    /// `{G}{G}` (Exact([{G},{G}])), `{2}{B}` (Exact([{2},{B}])),
    /// `{C}{U}` (Adarkar Wastes), `{W}{W}` (Codsworth).
    record Exact(List<ManaSymbol> symbols) implements Mana {}

    // ── Described shapes ─────────────────────────────────────────────

    /// `count` copies of the literal symbol bundle `symbols`, where
    /// `count` is *described* (variable, reference, for-each).
    /// "{C}{C} for each X" → Repeated(for-each-X, [{C},{C}]),
    /// "that much {C}" → Repeated(reference, [{C}]),
    /// "an amount of {G} equal to this creature's power"
    /// (Viridian Joiner) → Repeated(amount-eq-power, [{G}]).
    /// The literal-count case (no description) goes to [Exact].
    record Repeated(Amount count, List<ManaSymbol> symbols) implements Mana {}

    /// `count` copies of one color chosen at resolution from
    /// `palette`. "Three mana of any one color" (palette = WUBRG),
    /// "one mana of any color among …" (Mox Amber, palette from
    /// subjects), "one mana of any color a land you control could
    /// produce" (Reflecting Pool, palette from source).
    record OfOneColor(Amount count, Palette palette) implements Mana {}

    /// `count` mana, each chosen independently from `palette` —
    /// Manamorphose ("in any combination of colors"), Orcish
    /// Lumberjack ("in any combination of {R} and/or {G}").
    record Mixed(Amount count, Palette palette) implements Mana {}

    /// `count` distinct colors from `palette` — Firemind Vessel
    /// ("Add three mana of different colors"). Stricter than
    /// [Mixed]: picks must differ.
    record OfDistinctColors(Amount count, Palette palette) implements Mana {}

    /// `count` copies of the color named earlier in the same
    /// resolution — Sol Grail ("of the chosen color"), Caged Sun
    /// ("an additional one mana of that color").
    record OfThatColor(Amount count) implements Mana {}

    // ── Compound shapes ──────────────────────────────────────────────

    /// One of several alternatives. The choices are typically
    /// [Exact] but may be any [Mana]: "{G} or {U}" (Abandoned
    /// Campground) → AnyOf([Exact([{G}]), Exact([{U}])]); "{W}{W},
    /// {W}{U}, or {U}{U}" → AnyOf([Exact([{W},{W}]),
    /// Exact([{W},{U}]), Exact([{U},{U}])]); "{U} or {C}{U}"
    /// (Adarkar Unicorn) → AnyOf([Exact([{U}]), Exact([{C},{U}])]).
    /// Mirrors the existing [Cost.AnyOf] convention.
    record AnyOf(List<Mana> choices) implements Mana {}

    /// Compound: each part added in the same resolution event.
    /// Open the Omenpaths: "two mana of any one color and two mana
    /// of any other color" → AllOf([OfOneColor, OfOneColor]).
    /// Distinct from a sequence of separate `AddManaEffect`
    /// instances — this is one event, so a single
    /// [Restriction.SpendOnly] applies to the whole. Mirrors
    /// [Cost.AllOf].
    record AllOf(List<Mana> parts) implements Mana {}

    /// Wraps any [Mana] with a usage [Restriction]. Captures rule
    /// 106.6 (restrictions are tied to the produced mana itself,
    /// not to a sibling effect) — Adarkar Unicorn ("Add {U} or
    /// {C}{U}. Spend this mana only to pay cumulative upkeep
    /// costs.") becomes
    /// `Restricted(AnyOf([Exact([{U}]), Exact([{C},{U}])]),
    /// SpendOnly(...))`. Composes with [AnyOf] / [AllOf] so a
    /// single restriction binds to the whole bundle.
    record Restricted(Mana mana, Restriction restriction) implements Mana {}

    // ── Palette: where the choice set comes from ─────────────────────

    /// Color palette for [OfOneColor] / [Mixed] / [OfDistinctColors].
    sealed interface Palette {
        /// Static palette — five basic colors, or a restricted
        /// basic-color subset.
        record Explicit(List<ManaSymbol> symbols) implements Palette {}

        /// Dynamic — colors a source could (or did) produce
        /// (Squandered Resources, Reflecting Pool, Vorinclex,
        /// Benthic Explorers).
        record ProducedBy(Subject source) implements Palette {}

        /// Dynamic — colors *appearing on* a set of objects
        /// (Mox Amber: "any color among legendary creatures and
        /// planeswalkers you control").
        record AmongColorsOf(Subject objects) implements Palette {}
    }
}
