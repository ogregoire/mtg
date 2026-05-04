package be.imgn.mtg.engine.oracle2.parser;

import static be.imgn.mtg.engine.oracle2.parser.NumberParser.INTEGER;
import static be.imgn.mtg.engine.oracle2.parser.NumberParser.NUMBER;
import static be.imgn.mtg.engine.oracle2.parser.NumberParser.WORD_NUMBER;
import static be.imgn.mtg.engine.oracle2.parser.Parsers.phrase;
import static com.google.common.labs.parse.Parser.anyOf;
import static com.google.common.labs.parse.Parser.sequence;
import static com.google.common.labs.parse.Parser.word;

import com.google.common.labs.parse.Parser;

import be.imgn.mtg.engine.oracle2.domain.Amount;

/// Parsers for [Amount] expressions, built on the integer/word-number
/// atoms in [NumberParser]. Lives in `oracle2.parser` (not `selector`)
/// because numeric expressions are used across many oracle2 grammars
/// beyond selectors (effect costs, damage values, X definitions, etc.).
public final class AmountParser {
    private AmountParser() {}

    /// `Amount.Standard.X` — variable amount bound by the spell or
    /// ability's cost ({@mtg.rule 107.3}).
    private static final Parser<Amount> X_AMOUNT = word("X").thenReturn(Amount.Standard.X);

    /// `Amount.Standard.REFERENCE` — back-reference to a count
    /// established earlier (e.g. damage just dealt, cards just drawn).
    /// Spelled as "that much", "that many", or "that number" in oracle
    /// text — all collapse to the same domain value.
    private static final Parser<Amount> REFERENCE =
            phrase("that [much|many|number]").thenReturn(Amount.Standard.REFERENCE);

    /// "a" / "an" — exactly one. Title-or-lower per [Parsers#phrase].
    private static final Parser<Amount> ONE = phrase("a(n)").thenReturn(new Amount.Exact(1));

    /// Atoms allowed inside an [Amount.UpTo] bound — only literals and
    /// `X`. No nested `UpTo`, no `Range`, no `REFERENCE`.
    private static final Parser<Amount> UP_TO_INNER = anyOf(NUMBER.map(Amount.Exact::new), X_AMOUNT);

    /// "up to N" / "up to X" → [Amount.UpTo]`(Exact(N))` /
    /// `UpTo(Standard.X)`.
    private static final Parser<Amount.UpTo> UP_TO =
            phrase("up to").then(UP_TO_INNER).map(Amount.UpTo::new);

    /// "N or M" — inclusive range bounded on both sides.
    private static final Parser<Amount.Range> RANGE =
            sequence(NUMBER, word("or").then(NUMBER), Amount.Range::new);

    /// Top-level [Amount] entry. Order matters: ranged forms ("N or M")
    /// must precede bare integer forms so the trailing "or M" isn't
    /// left hanging for an outer "or".
    public static final Parser<Amount> AMOUNT = anyOf(
            UP_TO, RANGE, X_AMOUNT, REFERENCE, ONE, WORD_NUMBER.map(Amount.Exact::new), INTEGER.map(Amount.Exact::new));
}
