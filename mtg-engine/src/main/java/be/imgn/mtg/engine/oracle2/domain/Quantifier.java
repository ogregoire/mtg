package be.imgn.mtg.engine.oracle.domain2;

/// How many of the inner selector's matches we want — the count axis.
/// Splits into [StandardQuantifier] for bare-word quantifiers (all /
/// no / any number) and [Amount] for numeric counts (exact, up to,
/// range, X, that-many). One of the two; never both.
///
/// Composes with selectors via
/// `oracle.domain2.selector.QuantifierSelector`, which sits at the
/// top `Selector` level so a quantified selection can be a player,
/// an object, or a [target][be.imgn.mtg.engine.oracle.domain2.selector.Target].
public sealed interface Quantifier permits StandardQuantifier, Amount {}
