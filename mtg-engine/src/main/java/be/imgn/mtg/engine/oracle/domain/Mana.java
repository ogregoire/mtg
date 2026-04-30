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

    /// `count` mana, each chosen independently from `palette`. The
    /// `connector` records which connective spelling the oracle text
    /// used to enumerate the palette (Orcish Lumberjack: "{R} and/or
    /// {G}" → `AND_OR`; Manamorphose: "any combination of colors" → no
    /// enumerated symbols → `AND_OR` by convention since the implicit
    /// pick is inclusive).
    record Mixed(Amount count, Palette palette, Connector connector) implements Mana {
        /// Convenience for arms where the oracle text doesn't enumerate
        /// symbols (e.g., "in any combination of colors") — defaults
        /// to inclusive [Connector#AND_OR].
        public Mixed(Amount count, Palette palette) {
            this(count, palette, Connector.AND_OR);
        }
    }

    /// English connective spelling for an enumerated palette
    /// ("{R} and/or {G}" vs "{R} and {G}" vs "{R} or {G}"). Captured
    /// from oracle text and preserved on [Mixed]. In current
    /// well-formed oracle text the only spelling that appears in the
    /// "in any combination of" position is `AND_OR`, but the AST
    /// records what the parser actually saw.
    enum Connector {
        AND,
        OR,
        AND_OR
    }

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

        /// Dynamic potential palette — colors / types `source` *could*
        /// produce at the moment the ability resolves (rule 106.7
        /// future-snapshot of mana abilities). Reflecting Pool: "any
        /// type that a land you control could produce." Star Compass:
        /// "any color that a basic land you control could produce."
        /// Squandered Resources: "any type the sacrificed land could
        /// produce."  Benthic Explorers: "any type that land could
        /// produce." Distinct from [Produced] in that this captures
        /// the source's full mana-ability palette, not whatever a
        /// specific past tap event yielded. The `filter` discriminates
        /// "color" (exclude {C}) from "type" (include {C}) per
        /// rule 106.7.
        record CouldProduce(Subject source, Filter filter) implements Palette {}

        /// Dynamic past palette — colors / types `source` *actually*
        /// produced when last tapped for mana. Used by
        /// trigger-when-tapped abilities that mirror the produced
        /// type (Mirari's Wake, Sisay, Dictate of Karametra, Kinnan,
        /// Heartbeat of Spring's "that land produced"). Distinct
        /// from [CouldProduce]: a dual land tapped for `{U}` produced
        /// just `{U}`, not both halves. The `filter` mirrors
        /// [CouldProduce#filter].
        record Produced(Subject source, Filter filter) implements Palette {}

        /// "color" vs "type" filter on a dynamic palette (rule 106.7):
        /// `COLOR` restricts to the five basic colors (excludes {C}),
        /// `TYPE` includes the six basic types (W, U, B, R, G, C).
        enum Filter {
            COLOR,
            TYPE
        }

        /// Dynamic — colors *appearing on* a set of objects
        /// (Mox Amber: "any color among legendary creatures and
        /// planeswalkers you control").
        record AmongColorsOf(Subject objects) implements Palette {}
    }
}
