package be.imgn.mtg.engine.oracle2.parser.selector;

import static be.imgn.mtg.engine.oracle2.parser.Parsers.phrase;
import static com.google.common.labs.parse.Parser.anyOf;

import java.util.function.Function;

import com.google.common.labs.parse.Parser;

import be.imgn.mtg.engine.oracle2.domain.AmountMatcher;
import be.imgn.mtg.engine.oracle2.domain.selector.ManaCostSelector;
import be.imgn.mtg.engine.oracle2.domain.selector.ObjectPropertySelector;

/// Parser for [ManaCostSelector].
///
/// - [ManaCostSelector.Standard#CHOSEN] — "with mana value of the
///   chosen quality" (Ashling's Prerogative, Extinction Event, …).
/// - [ManaCostSelector.SharesManaValueWith] — "with the same mana
///   value as X".
/// - [ManaCostSelector.HasManaValue] — "with mana value [matcher]"
///   (Abrupt Decay, Angry Rabble, …) — exposed via
///   [#MANA_VALUE_ASPECT] (package-private) for [NumericAspectParser]
///   to consume in its shared-matcher disjunction logic.
public final class ManaCostSelectorParser {
    private ManaCostSelectorParser() {}

    /// Aspect contribution to [NumericAspectParser]: matches the
    /// keyword `mana value` and returns the wrapping function from
    /// [AmountMatcher] to [ManaCostSelector.HasManaValue].
    static final Parser<Function<AmountMatcher, ObjectPropertySelector>> MANA_VALUE_ASPECT =
            phrase("mana value").thenReturn(ManaCostSelector.HasManaValue::new);

    /// "with mana value of the chosen quality" —
    /// [ManaCostSelector.Standard#CHOSEN].
    private static final Parser<ManaCostSelector.Standard> CHOSEN =
            phrase("with mana value of the chosen quality").thenReturn(ManaCostSelector.Standard.CHOSEN);

    /// "with the same mana value as X" —
    /// [ManaCostSelector.SharesManaValueWith]. Recursive on
    /// [be.imgn.mtg.engine.oracle2.domain.selector.ObjectSelector].
    private static final Parser<ManaCostSelector.SharesManaValueWith> SHARES_MANA_VALUE_WITH = phrase(
                    "with the same mana value as")
            .then(Refs.OBJECT_SELECTOR)
            .map(ManaCostSelector.SharesManaValueWith::new);

    /// Top-level [ManaCostSelector]. `SharesManaValueWith` first
    /// (longer specific prefix "with the same mana value as"), then
    /// `Chosen`. The numeric `HasManaValue` form is contributed to
    /// [NumericAspectParser] via [#MANA_VALUE_ASPECT].
    public static final Parser<ManaCostSelector> MANA_COST_SELECTOR = anyOf(SHARES_MANA_VALUE_WITH, CHOSEN);
}
