package be.imgn.mtg.engine.oracle2.parser.selector;

import static be.imgn.mtg.engine.oracle2.parser.Parsers.phrase;

import java.util.function.Function;

import com.google.common.labs.parse.Parser;

import be.imgn.mtg.engine.oracle2.domain.AmountMatcher;
import be.imgn.mtg.engine.oracle2.domain.selector.ObjectPropertySelector;
import be.imgn.mtg.engine.oracle2.domain.selector.PowerSelector;

/// Parser for [PowerSelector].
///
/// - [PowerSelector.SharesPowerWith] — "with the same power as X" —
///   wired into [#POWER_SELECTOR] for the per-axis dispatch.
/// - [PowerSelector.HasPower] — "with power [matcher]" — exposed via
///   [#POWER_ASPECT] (package-private) for [NumericAspectParser] to
///   consume in its shared-matcher disjunction logic. The numeric
///   comparison form is dispatched there because oracle phrasing
///   like "with power or toughness 1 or less" distributes a single
///   matcher across multiple aspects.
public final class PowerSelectorParser {
    private PowerSelectorParser() {}

    /// Aspect contribution to [NumericAspectParser]: matches the
    /// keyword `power` and returns the wrapping function from
    /// [AmountMatcher] to [PowerSelector.HasPower].
    static final Parser<Function<AmountMatcher, ObjectPropertySelector>> POWER_ASPECT =
            phrase("power").thenReturn(PowerSelector.HasPower::new);

    /// "with the same power as X" — [PowerSelector.SharesPowerWith].
    private static final Parser<PowerSelector.SharesPowerWith> SHARES_POWER_WITH =
            phrase("with the same power as").then(Refs.OBJECT_SELECTOR).map(PowerSelector.SharesPowerWith::new);

    /// Top-level [PowerSelector]. Currently a single arm — the
    /// numeric `HasPower` form is contributed to
    /// [NumericAspectParser] via [#POWER_ASPECT].
    public static final Parser<PowerSelector> POWER_SELECTOR = SHARES_POWER_WITH.map(s -> s);
}
