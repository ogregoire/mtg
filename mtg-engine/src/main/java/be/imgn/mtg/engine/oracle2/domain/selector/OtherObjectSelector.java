package be.imgn.mtg.engine.oracle2.domain.selector;

import static java.util.Objects.requireNonNull;

/// "Y is an object other than X" — selects an object different from
/// `than`. Object-side counterpart to [OtherPlayerSelector]. The
/// reference is typically [SelfSelector#SELF] (`~`) when oracle text
/// says "another creature" with no antecedent, but the parser may
/// bind it to a previously-mentioned object instead.
///
/// Combined with [QuantifierSelector] for the canonical mappings:
/// - `QuantifierSelector(Amount.Exact(1), OtherObjectSelector(...))` is "another X"
/// - `QuantifierSelector(StandardQuantifier.ALL, OtherObjectSelector(...))` is "other Xs"
public record OtherObjectSelector(ObjectSelector than) implements ObjectPropertySelector {
    public OtherObjectSelector {
        requireNonNull(than);
    }
}
