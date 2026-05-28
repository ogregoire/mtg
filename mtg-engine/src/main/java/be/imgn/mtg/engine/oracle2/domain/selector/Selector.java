package be.imgn.mtg.engine.oracle2.domain.selector;

import static java.util.Objects.requireNonNull;

import java.util.List;

/// Root of the selector hierarchy: anything an oracle-text phrase can
/// pick out. Splits into [PlayerSelector] for player slots,
/// [ObjectSelector] for objects (cards, permanents, spells, …),
/// [QuantifierSelector] which adds a count axis on top of any of the
/// above ("two creatures", "each opponent", …), [AllOf] which
/// composes multiple selectors via top-level "and" coordination
/// ("each creature without flying and each player"), [OneOf]
/// which expresses a union of alternatives at the selector level
/// for the rare cross-axis "target X or Y" form (Firesong and
/// Sunspeaker: "target creature or player"), and [OneOrMoreOf]
/// which expresses the "non-empty subset" semantics of `and/or`
/// (Chaotic Transformation: "up to one target artifact, …, and/or
/// up to one target land"). The "target" marker (CR 115.1) lives
/// per-axis as [PlayerSelector.Target] / [ObjectSelector.Target]
/// so each Target is naturally typed by the thing it wraps.
public sealed interface Selector
        permits PlayerSelector,
                ObjectSelector,
                QuantifierSelector,
                Selector.AllOf,
                Selector.OneOf,
                Selector.OneOrMoreOf {

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

    /// Top-level "or"-union: exactly one target chosen from
    /// heterogeneous alternatives, as in "target creature **or**
    /// player" (Firesong and Sunspeaker). Distinct from
    /// [ObjectPropertySelector.OneOf] (which composes properties on
    /// a single object) — this composes whole selectors of
    /// potentially different axes (object vs player). Single-
    /// element lists are never wrapped in `OneOf`.
    record OneOf(List<Selector> selectors) implements Selector {
        public OneOf {
            requireNonNull(selectors);
            selectors = List.copyOf(selectors);
            if (selectors.size() < 2) {
                throw new IllegalArgumentException("Selector.OneOf needs at least 2 elements, got " + selectors.size());
            }
        }
    }

    /// Top-level "and/or"-list: any non-empty subset of the listed
    /// selectors is picked, each independently. Distinct from
    /// [AllOf] (all of them taken together) and [OneOf] (exactly
    /// one picked). Chaotic Transformation: "up to one target
    /// artifact, …, and/or up to one target land" →
    /// `OneOrMoreOf([Q(UpTo(1), Target(art)), …, Q(UpTo(1),
    /// Target(land))])`. Each slot is independently satisfied
    /// (`up to one` permits zero per slot); `and/or` only requires
    /// at least one slot be filled across the whole list.
    /// Single-element lists are never wrapped in `OneOrMoreOf`.
    record OneOrMoreOf(List<Selector> selectors) implements Selector {
        public OneOrMoreOf {
            selectors = List.copyOf(selectors);
            if (selectors.size() < 2) {
                throw new IllegalArgumentException(
                        "Selector.OneOrMoreOf needs at least 2 elements, got " + selectors.size());
            }
        }
    }
}
