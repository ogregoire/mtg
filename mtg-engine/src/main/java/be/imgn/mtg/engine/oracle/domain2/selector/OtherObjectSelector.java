package be.imgn.mtg.engine.oracle.domain2.selector;

import java.util.Objects;

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
        Objects.requireNonNull(than);
    }
}
