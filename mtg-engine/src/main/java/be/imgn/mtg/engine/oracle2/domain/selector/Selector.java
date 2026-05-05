package be.imgn.mtg.engine.oracle2.domain.selector;

import static java.util.Objects.requireNonNull;

import java.util.List;

/// Root of the selector hierarchy: anything an oracle-text phrase can
/// pick out. Splits into [PlayerSelector] for player slots,
/// [ObjectSelector] for objects (cards, permanents, spells, …),
/// [QuantifierSelector] which adds a count axis on top of any of the
/// above ("two creatures", "each opponent", …), [AllOf] which
/// composes multiple selectors via top-level "and" coordination
/// ("each creature without flying and each player"), and [AnyOf]
/// which expresses a union of alternatives at the selector level
/// for the rare cross-axis "target X or Y" form (Firesong and
/// Sunspeaker: "target creature or player"). The "target" marker
/// (CR 115.1) lives per-axis as [PlayerSelector.Target] /
/// [ObjectSelector.Target] so each Target is naturally typed by the
/// thing it wraps.
public sealed interface Selector
        permits PlayerSelector, ObjectSelector, QuantifierSelector, Selector.AllOf, Selector.AnyOf {

    /// Top-level "and"-list: two or more selectors taken together,
    /// as in "deal damage to each creature without flying **and**
    /// each player". The combined targets are all picked out
    /// simultaneously — semantically a list, not a disjunction.
    /// Single-element lists are never wrapped in `AllOf`; the bare
    /// selector is returned directly.
    record AllOf(List<Selector> selectors) implements Selector {
        public AllOf {
            requireNonNull(selectors);
            selectors = List.copyOf(selectors);
            if (selectors.size() < 2) {
                throw new IllegalArgumentException("Selector.AllOf needs at least 2 elements, got " + selectors.size());
            }
        }
    }

    /// Top-level "or"-union: one target chosen from heterogeneous
    /// alternatives, as in "target creature **or** player" (Firesong
    /// and Sunspeaker). Distinct from
    /// [ObjectPropertySelector.AnyOf] (which composes properties on
    /// a single object) — this composes whole selectors of
    /// potentially different axes (object vs player). Single-
    /// element lists are never wrapped in `AnyOf`.
    record AnyOf(List<Selector> selectors) implements Selector {
        public AnyOf {
            requireNonNull(selectors);
            selectors = List.copyOf(selectors);
            if (selectors.size() < 2) {
                throw new IllegalArgumentException("Selector.AnyOf needs at least 2 elements, got " + selectors.size());
            }
        }
    }
}
