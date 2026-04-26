package be.imgn.mtg.engine.oracle.domain;

import java.util.List;

/// Top-level wrapper around a [Selector] that admits selector-level
/// disjunction. Most selectors are a single [Single] arm; the [Or]
/// arm is reserved for cases where alternatives differ across multiple
/// axes simultaneously and cannot be expressed as a single matcher's
/// `Any` combinator.
///
/// Examples that stay as [Single]:
/// - "target creature" (one selector)
/// - "target creature or planeswalker" — single-axis disjunction
///   collapses into one selector with `CardTypes(Any[Is, Is])`
///
/// Examples that require [Or]:
/// - "target Goblin creature or Knight" — branch 1 has subtype + card-
///   type predicates, branch 2 has only a subtype predicate; cannot be
///   represented as a single boolean tree per axis
public sealed interface SelectorExpression {

    /// One [Selector] — the common case.
    record Single(Selector selector) implements SelectorExpression {}

    /// Disjunction of two or more selectors with structurally distinct
    /// shapes. Reserved for the cross-axis case the matcher trees
    /// can't express.
    record Or(List<Selector> alternatives) implements SelectorExpression {}
}
