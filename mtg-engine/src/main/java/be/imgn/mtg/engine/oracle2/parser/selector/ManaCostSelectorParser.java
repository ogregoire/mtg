package be.imgn.mtg.engine.oracle2.parser.selector;

import static be.imgn.mtg.engine.oracle2.parser.AmountMatcherParser.AMOUNT_MATCHER;
import static be.imgn.mtg.engine.oracle2.parser.Parsers.phrase;
import static com.google.common.labs.parse.Parser.anyOf;

import com.google.common.labs.parse.Parser;

import be.imgn.mtg.engine.oracle2.domain.selector.ManaCostSelector;

/// Parser for [ManaCostSelector]. Three arms wired:
///
/// 1. [ManaCostSelector.HasManaValue] — "with mana value [matcher]"
///    (Abrupt Decay, Angry Rabble, As Foretold, …).
/// 2. [ManaCostSelector.Chosen] — "with mana value of the chosen
///    quality" (Ashling's Prerogative, Extinction Event, …).
/// 3. [ManaCostSelector.SharesManaValueWith] — "with the same mana
///    value as X".
public final class ManaCostSelectorParser {
    private ManaCostSelectorParser() {}

    /// "with mana value of the chosen quality" —
    /// [ManaCostSelector.Chosen] with slot `"quality"`.
    private static final Parser<ManaCostSelector.Chosen> CHOSEN =
            phrase("with mana value of the chosen quality").thenReturn(new ManaCostSelector.Chosen("quality"));

    /// "with the same mana value as X" —
    /// [ManaCostSelector.SharesManaValueWith]. Recursive on
    /// [be.imgn.mtg.engine.oracle2.domain.selector.ObjectSelector].
    private static final Parser<ManaCostSelector.SharesManaValueWith> SHARES_MANA_VALUE_WITH = phrase(
                    "with the same mana value as")
            .then(Refs.OBJECT_SELECTOR)
            .map(ManaCostSelector.SharesManaValueWith::new);

    /// "with mana value [matcher]" — [ManaCostSelector.HasManaValue].
    /// The matcher consumes "N or [more|less|greater]", "exactly N",
    /// "at least N", "at most N", "no", or bare "N".
    private static final Parser<ManaCostSelector.HasManaValue> HAS_MANA_VALUE =
            phrase("with mana value").then(AMOUNT_MATCHER).map(ManaCostSelector.HasManaValue::new);

    /// Top-level [ManaCostSelector]. Order: `SharesManaValueWith`
    /// first (longest specific prefix "with the same mana value as"),
    /// then `Chosen` (specific "of the chosen quality" tail), then
    /// `HasManaValue` (generic numeric matcher).
    public static final Parser<ManaCostSelector> MANA_COST_SELECTOR =
            anyOf(SHARES_MANA_VALUE_WITH, CHOSEN, HAS_MANA_VALUE);
}
