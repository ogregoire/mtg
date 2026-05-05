package be.imgn.mtg.engine.oracle2.domain.selector;

import static java.util.Objects.requireNonNull;

import be.imgn.mtg.engine.oracle2.domain.Quantifier;

/// Wraps a [Selector] with a [Quantifier] count. Sits at the top
/// [Selector] level; the inner `selector` can be a [PlayerSelector]
/// or [ObjectSelector] — directly, or wrapped in the matching
/// per-axis Target ([PlayerSelector.Target] /
/// [ObjectSelector.Target]) for targeted-with-count phrases like
/// "two target creatures."
///
/// The outer wrapper is `QuantifierSelector` so each individual
/// target inside a `Target(...)` is itself a target. Conventional
/// shape: `QuantifierSelector(Amount.Exact(2), ObjectSelector.Target(creature))`,
/// not `ObjectSelector.Target(QuantifierSelector(Amount.Exact(2), creature))`.
///
/// Canonical relational mappings:
/// - "another X" → `QuantifierSelector(Amount.Exact(1), Other<X>(reference))`
/// - "other Xs"  → `QuantifierSelector(StandardQuantifier.ALL, Other<X>(reference))`
public record QuantifierSelector(Quantifier quantifier, Selector selector) implements Selector {
    public QuantifierSelector {
        requireNonNull(quantifier);
        requireNonNull(selector);
    }
}
