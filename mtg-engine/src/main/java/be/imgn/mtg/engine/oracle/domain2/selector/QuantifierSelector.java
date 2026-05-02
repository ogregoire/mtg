package be.imgn.mtg.engine.oracle.domain2.selector;

import java.util.Objects;

import be.imgn.mtg.engine.oracle.domain2.Quantifier;

/// Wraps a [Selector] with a [Quantifier] count. Sits at the top
/// [Selector] level; the inner `selector` can be a [PlayerSelector],
/// an [ObjectSelector], or a [Target] — `Target` is the canonical
/// layering for targeted-with-count phrases like "two target
/// creatures."
///
/// The outer wrapper is `QuantifierSelector` so each individual
/// target inside a `Target(...)` is itself a target. Conventional
/// shape: `QuantifierSelector(Amount.Exact(2), Target(creature))`,
/// not `Target(QuantifierSelector(Amount.Exact(2), creature))`.
///
/// Canonical relational mappings:
/// - "another X" → `QuantifierSelector(Amount.Exact(1), Other<X>(reference))`
/// - "other Xs"  → `QuantifierSelector(StandardQuantifier.ALL, Other<X>(reference))`
public record QuantifierSelector(Quantifier quantifier, Selector selector) implements Selector {
    public QuantifierSelector {
        Objects.requireNonNull(quantifier);
        Objects.requireNonNull(selector);
    }
}
