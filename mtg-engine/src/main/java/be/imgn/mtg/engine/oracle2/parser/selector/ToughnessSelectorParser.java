package be.imgn.mtg.engine.oracle2.parser.selector;

import static be.imgn.mtg.engine.oracle2.parser.Parsers.phrase;

import java.util.function.Function;

import com.google.common.labs.parse.Parser;

import be.imgn.mtg.engine.oracle2.domain.AmountMatcher;
import be.imgn.mtg.engine.oracle2.domain.selector.ObjectPropertySelector;
import be.imgn.mtg.engine.oracle2.domain.selector.ToughnessSelector;

/// Parser for [ToughnessSelector].
///
/// - [ToughnessSelector.SharesToughnessWith] — "with the same
///   toughness as X" — wired into [#TOUGHNESS_SELECTOR].
/// - [ToughnessSelector.HasToughness] — "with toughness [matcher]"
///   — exposed via [#TOUGHNESS_ASPECT] (package-private) for
///   [NumericAspectParser] to consume in its shared-matcher
///   disjunction logic.
public final class ToughnessSelectorParser {
    private ToughnessSelectorParser() {}

    /// Aspect contribution to [NumericAspectParser]: matches the
    /// keyword `toughness` and returns the wrapping function from
    /// [AmountMatcher] to [ToughnessSelector.HasToughness].
    static final Parser<Function<AmountMatcher, ObjectPropertySelector>> TOUGHNESS_ASPECT =
            phrase("toughness").thenReturn(ToughnessSelector.HasToughness::new);

    /// "with the same toughness as X" — [ToughnessSelector.SharesToughnessWith].
    private static final Parser<ToughnessSelector.SharesToughnessWith> SHARES_TOUGHNESS_WITH = phrase(
                    "with the same toughness as")
            .then(Refs.OBJECT_SELECTOR)
            .map(ToughnessSelector.SharesToughnessWith::new);

    /// Top-level [ToughnessSelector]. Currently a single arm — the
    /// numeric `HasToughness` form is contributed to
    /// [NumericAspectParser] via [#TOUGHNESS_ASPECT].
    public static final Parser<ToughnessSelector> TOUGHNESS_SELECTOR = SHARES_TOUGHNESS_WITH.map(s -> s);
}
