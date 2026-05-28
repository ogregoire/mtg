package be.imgn.mtg.engine.oracle2.domain.mana;

import java.util.List;

import org.jspecify.annotations.Nullable;

import be.imgn.mtg.engine.oracle2.domain.Amount;

/// The mana an [be.imgn.mtg.engine.oracle2.domain.effect.AddManaEffect]
/// produces in one resolution. Mirrors the legacy
/// `oracle.domain.Mana` umbrella:
///
/// **Literal shapes** —
/// - [Exact] — an ordered, fixed list of individual [Mana] instances
///   (`Add {G}{G}.` → `Exact([Mana({G}), Mana({G})])`).
/// - [Repeated] — `count` copies of a literal symbol bundle where
///   `count` is *described* (`{C}{C} for each X`, `that much {C}`,
///   `an amount of {G} equal to power`).
///
/// **Choose-one-of-many shapes** (the resolver picks at resolution,
/// {@mtg.rule 106.1c}) —
/// - [OfOneColor] — `count` copies of one colour chosen from a
///   [Palette] ("three mana of any one colour").
/// - [OfThatColor] — `count` copies of the colour named earlier in
///   the same resolution ("an additional one mana of that colour").
/// - [OfDistinctColors] — `count` distinct colours from a [Palette]
///   (Firemind Vessel).
/// - [Mixed] — `count` mana each chosen independently from a
///   [Palette] (Manamorphose, Orcish Lumberjack).
///
/// **Compound shapes** —
/// - [OneOf] — alternatives the controller chooses among at
///   resolution (`Add {R} or {G}.`).
/// - [AllOf] — multiple parts added in the same event (Open the
///   Omenpaths: "two mana of any one colour and two mana of any
///   other colour").
///
/// Spend restrictions ({@mtg.rule 106.6}) attach via
/// [#withRestriction(Restriction)] — for [Exact] they distribute per
/// [Mana] instance; for [OneOf]/[AllOf] they distribute recursively
/// into each child; for the dynamic shapes they sit on a per-shape
/// `@Nullable Restriction` slot since the individual instances are
/// only materialised at resolution.
public sealed interface ProducedMana {

    /// Returns a [ProducedMana] in which every produced [Mana]
    /// instance carries the given restriction. Implementations
    /// either distribute per-instance (literal shapes), per-arm
    /// (compounds), or attach to a per-shape restriction slot
    /// (dynamic shapes).
    ProducedMana withRestriction(Restriction restriction);

    // ── Literal shapes ─────────────────────────────────────────────

    /// Ordered, fixed list of [Mana] instances — the "concatenation"
    /// shape used by literal payloads like `{G}{G}` or `{2}{B}`.
    record Exact(List<Mana> instances) implements ProducedMana {
        public Exact {
            instances = List.copyOf(instances);
            if (instances.isEmpty()) {
                throw new IllegalArgumentException("ProducedMana.Exact must contain at least one Mana instance");
            }
        }

        @Override
        public Exact withRestriction(Restriction restriction) {
            return new Exact(
                    instances.stream().map(m -> m.withRestriction(restriction)).toList());
        }
    }

    /// `count` copies of the literal `symbols` bundle, where `count`
    /// is *described* — variable ("for each X"), back-reference
    /// ("that much"), property ("an amount of `{G}` equal to this
    /// creature's power"). The literal-count case (no description) is
    /// covered by [Exact].
    record Repeated(
            Amount count,
            List<ManaSymbol> symbols,
            @Nullable Restriction restriction) implements ProducedMana {
        public Repeated {
            symbols = List.copyOf(symbols);
            if (symbols.isEmpty()) {
                throw new IllegalArgumentException("ProducedMana.Repeated must contain at least one symbol");
            }
        }

        public Repeated(Amount count, List<ManaSymbol> symbols) {
            this(count, symbols, null);
        }

        @Override
        public Repeated withRestriction(Restriction restriction) {
            return new Repeated(count, symbols, restriction);
        }
    }

    // ── Choose-one-of-many shapes ──────────────────────────────────

    /// `count` copies of one colour chosen at resolution from
    /// `palette`. "Three mana of any one colour" (palette = WUBRG),
    /// "one mana of any colour among …" (Mox Amber when added —
    /// today only [Palette.Explicit] is modelled).
    record OfOneColor(
            Amount count, Palette palette, @Nullable Restriction restriction) implements ProducedMana {
        public OfOneColor(Amount count, Palette palette) {
            this(count, palette, null);
        }

        @Override
        public OfOneColor withRestriction(Restriction restriction) {
            return new OfOneColor(count, palette, restriction);
        }
    }

    /// `count` copies of the colour named earlier in the same
    /// resolution — Sol Grail ("of the chosen colour"), Caged Sun
    /// ("an additional one mana of that colour").
    record OfThatColor(Amount count, @Nullable Restriction restriction) implements ProducedMana {
        public OfThatColor(Amount count) {
            this(count, null);
        }

        @Override
        public OfThatColor withRestriction(Restriction restriction) {
            return new OfThatColor(count, restriction);
        }
    }

    /// `count` distinct colours from `palette` — Firemind Vessel
    /// ("add three mana of different colours"). Stricter than
    /// [Mixed]: picks must differ.
    record OfDistinctColors(
            Amount count, Palette palette, @Nullable Restriction restriction) implements ProducedMana {
        public OfDistinctColors(Amount count, Palette palette) {
            this(count, palette, null);
        }

        @Override
        public OfDistinctColors withRestriction(Restriction restriction) {
            return new OfDistinctColors(count, palette, restriction);
        }
    }

    /// `count` mana each chosen independently from `palette`. The
    /// `connector` records which English connective the oracle text
    /// used to enumerate the palette ("{R} and/or {G}" → `AND_OR`,
    /// "any combination of colours" → `AND_OR` by convention).
    record Mixed(
            Amount count,
            Palette palette,
            Connector connector,
            @Nullable Restriction restriction) implements ProducedMana {
        public Mixed(Amount count, Palette palette) {
            this(count, palette, Connector.AND_OR, null);
        }

        public Mixed(Amount count, Palette palette, Connector connector) {
            this(count, palette, connector, null);
        }

        @Override
        public Mixed withRestriction(Restriction restriction) {
            return new Mixed(count, palette, connector, restriction);
        }
    }

    /// English connective spelling for an enumerated palette
    /// ("{R} and/or {G}" vs "{R} and {G}" vs "{R} or {G}"). Captured
    /// from oracle text and preserved on [Mixed].
    enum Connector {
        AND,
        OR,
        AND_OR
    }

    // ── Compound shapes ────────────────────────────────────────────

    /// Alternatives the controller chooses among at resolution
    /// ({@mtg.rule 106.1c}: "if an ability lets you choose one of
    /// several types of mana, you choose at the time the mana is
    /// produced"). Carries ≥2 alternatives; singletons collapse to
    /// the bare arm at parse time.
    record OneOf(List<ProducedMana> alternatives) implements ProducedMana {
        public OneOf {
            alternatives = List.copyOf(alternatives);
            if (alternatives.size() < 2) {
                throw new IllegalArgumentException(
                        "ProducedMana.OneOf needs at least 2 alternatives, got " + alternatives.size());
            }
        }

        @Override
        public OneOf withRestriction(Restriction restriction) {
            return new OneOf(alternatives.stream()
                    .map(a -> a.withRestriction(restriction))
                    .toList());
        }
    }

    /// Compound: each `part` added in the same resolution event. Open
    /// the Omenpaths: "two mana of any one colour **and** two mana of
    /// any other colour" → `AllOf([OfOneColor, OfOneColor])`. Distinct
    /// from emitting two separate [AddManaEffect][be.imgn.mtg.engine.oracle2.domain.effect.AddManaEffect]
    /// sentences — this is one resolution event, so a single
    /// [Restriction] applies to the whole bundle.
    record AllOf(List<ProducedMana> parts) implements ProducedMana {
        public AllOf {
            parts = List.copyOf(parts);
            if (parts.size() < 2) {
                throw new IllegalArgumentException("ProducedMana.AllOf needs at least 2 parts, got " + parts.size());
            }
        }

        @Override
        public AllOf withRestriction(Restriction restriction) {
            return new AllOf(
                    parts.stream().map(p -> p.withRestriction(restriction)).toList());
        }
    }
}
