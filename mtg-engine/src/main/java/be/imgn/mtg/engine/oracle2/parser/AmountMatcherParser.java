package be.imgn.mtg.engine.oracle2.parser;

import static be.imgn.mtg.engine.oracle2.parser.NumberParser.NUMBER;
import static be.imgn.mtg.engine.oracle2.parser.Parsers.phrase;
import static com.google.common.labs.parse.Parser.anyOf;
import static com.google.common.labs.parse.Parser.word;

import com.google.common.labs.parse.Parser;

import be.imgn.mtg.engine.oracle2.domain.Amount;
import be.imgn.mtg.engine.oracle2.domain.AmountMatcher;

/// Parser for [AmountMatcher] — relational predicates over a
/// quantity ("N or greater", "at most N", "exactly N", bare "N",
/// "no"). Used wherever oracle text compares a count to a bound:
/// `ManaCostSelector.HasManaValue`, future Power/Toughness/Loyalty
/// selectors, condition expressions, etc.
///
/// The bound is an [Amount], restricted here to literals
/// ([Amount.Exact]) and the variable [Amount.Standard#X]. Other
/// `Amount` shapes (`UpTo`, `Range`) are not meaningful inside a
/// matcher.
public final class AmountMatcherParser {
    private AmountMatcherParser() {}

    /// Atoms allowed inside an [AmountMatcher] bound — only literals
    /// and `X`.
    private static final Parser<Amount> AMOUNT_ATOM =
            anyOf(NUMBER.<Amount>map(Amount.Exact::new), word("X").thenReturn(Amount.Standard.X));

    /// "N or [less|more|greater]" — postfix comparator. "more" and
    /// "greater" both map to `AtLeast`; "less" maps to `AtMost`.
    private static final Parser<AmountMatcher> POSTFIX_COMPARATOR = anyOf(
            AMOUNT_ATOM.followedBy(phrase("or less")).map(AmountMatcher.AtMost::new),
            AMOUNT_ATOM.followedBy(phrase("or [more|greater]")).map(AmountMatcher.AtLeast::new));

    /// "at least N" — prefix `AtLeast`.
    private static final Parser<AmountMatcher> AT_LEAST =
            phrase("at least").then(AMOUNT_ATOM).map(AmountMatcher.AtLeast::new);

    /// "at most N" — prefix `AtMost`.
    private static final Parser<AmountMatcher> AT_MOST =
            phrase("at most").then(AMOUNT_ATOM).map(AmountMatcher.AtMost::new);

    /// "exactly N" — explicit `Exactly`.
    private static final Parser<AmountMatcher> EXACTLY =
            phrase("exactly").then(AMOUNT_ATOM).map(AmountMatcher.Exactly::new);

    /// "no" — folds to `Exactly(0)`.
    private static final Parser<AmountMatcher> NONE =
            word("no").thenReturn(new AmountMatcher.Exactly(new Amount.Exact(0)));

    /// Bare amount → `Exactly(amount)`. Last so longer prefix forms win.
    private static final Parser<AmountMatcher> BARE = AMOUNT_ATOM.map(AmountMatcher.Exactly::new);

    /// Top-level [AmountMatcher]. Order: postfix-comparator (longest
    /// match — consumes the trailing "or X" tail), then explicit
    /// prefixes, then "no", then bare amount.
    public static final Parser<AmountMatcher> AMOUNT_MATCHER =
            anyOf(POSTFIX_COMPARATOR, AT_LEAST, AT_MOST, EXACTLY, NONE, BARE);
}
