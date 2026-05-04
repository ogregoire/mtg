package be.imgn.mtg.engine.oracle2.parser.selector;

import static be.imgn.mtg.engine.oracle2.parser.CardNameParser.CARD_NAME;
import static be.imgn.mtg.engine.oracle2.parser.Parsers.phrase;
import static com.google.common.labs.parse.Parser.anyOf;

import com.google.common.labs.parse.Parser;

import be.imgn.mtg.engine.oracle2.domain.selector.NameSelector;

/// Parser for [NameSelector]. Four arms:
///
/// 1. [NameSelector.Is] — "named X" / "with the name X". Literal card-
///    name match via [be.imgn.mtg.engine.oracle2.parser.CardNameParser#CARD_NAME].
/// 2. [NameSelector.SharesNameWith] — "with the same name as X".
///    Recursive on [be.imgn.mtg.engine.oracle2.domain.selector.ObjectSelector].
/// 3. [NameSelector.Chosen] — "with the chosen name". Back-reference
///    to a name slot bound by an earlier ChooseName effect.
/// 4. [NameSelector.HasNoName] — "with no name". Face-down creatures
///    have no name ({@mtg.rule 707.2}).
///
/// "Not named X" is handled at the [PropertyParser] level via
/// [be.imgn.mtg.engine.oracle2.domain.selector.ObjectPropertySelector.Not].
public final class NameSelectorParser {
    private NameSelectorParser() {}

    /// "named X" — [NameSelector.Is] over a literal card name.
    private static final Parser<NameSelector.Is> NAMED =
            phrase("named").then(CARD_NAME).map(NameSelector.Is::new);

    /// "with the same name as X" — [NameSelector.SharesNameWith].
    private static final Parser<NameSelector.SharesNameWith> SHARES_NAME_WITH =
            phrase("with the same name as").then(Refs.OBJECT_SELECTOR).map(NameSelector.SharesNameWith::new);

    /// "with the chosen name" — [NameSelector.Chosen] with slot
    /// `"name"`.
    private static final Parser<NameSelector.Chosen> CHOSEN =
            phrase("with the chosen name").thenReturn(new NameSelector.Chosen("name"));

    /// "that [don't|doesn't] have a name" — [NameSelector.HasNoName].
    /// Sole vintage-legal use is Pompous Gadabout: "creatures that
    /// don't have a name". Covers face-down creatures
    /// ({@mtg.rule 707.2}) and any creature whose name has been
    /// removed by an effect.
    private static final Parser<NameSelector.HasNoName> HAS_NO_NAME =
            phrase("that [don't|doesn't] have a name").thenReturn(new NameSelector.HasNoName());

    /// Top-level [NameSelector]. Order matters:
    /// 1. [#SHARES_NAME_WITH] — longest specific multi-word prefix.
    /// 2. [#CHOSEN] / [#HAS_NO_NAME] — multi-word literal phrases.
    /// 3. [#NAMED] — generic "named X" form.
    public static final Parser<NameSelector> NAME_SELECTOR = anyOf(SHARES_NAME_WITH, CHOSEN, HAS_NO_NAME, NAMED);
}
