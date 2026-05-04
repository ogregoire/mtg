package be.imgn.mtg.engine.oracle2.parser;

import static be.imgn.mtg.engine.oracle2.parser.Parsers.phrase;
import static com.google.common.labs.parse.Parser.anyOf;
import static com.google.common.labs.parse.Parser.digits;
import static com.google.common.labs.parse.Parser.one;
import static com.google.common.labs.parse.Parser.sequence;

import com.google.common.labs.parse.Parser;
import com.google.mu.util.CharPredicate;

/// Numeric atoms used across the oracle2 parser tree. Returns plain
/// `Integer`s — wrap in [be.imgn.mtg.engine.oracle2.domain.Amount.Exact]
/// at the call site when an [be.imgn.mtg.engine.oracle2.domain.Amount]
/// is needed.
///
/// Three flavors:
/// - [#INTEGER] — unsigned digit literal (`0`, `42`).
/// - [#WORD_NUMBER] — English `One` … `Twenty`, title-or-lower.
/// - [#NUMBER] — either form ([#WORD_NUMBER] tried first, then [#INTEGER]).
/// - [#SIGNED_INT] — explicit `+N` / `-N` / `−N`. The minus
///   alternative accepts both ASCII `-` (P/T markers) and U+2212
///   (Scryfall planeswalker loyalty costs).
public final class NumberParser {
    private NumberParser() {}

    /// Plain digit literal — `0`, `1`, `42`, …
    public static final Parser<Integer> INTEGER = digits().map(Integer::parseInt);

    /// English word number `One` … `Twenty`. Title-or-lower per
    /// [Parsers#phrase]. Listed in descending order so the longer
    /// `-teen` forms naturally precede their short counterparts
    /// (`Sixteen` before `Six`, etc.).
    public static final Parser<Integer> WORD_NUMBER = anyOf(
            phrase("Twenty").thenReturn(20),
            phrase("Nineteen").thenReturn(19),
            phrase("Eighteen").thenReturn(18),
            phrase("Seventeen").thenReturn(17),
            phrase("Sixteen").thenReturn(16),
            phrase("Fifteen").thenReturn(15),
            phrase("Fourteen").thenReturn(14),
            phrase("Thirteen").thenReturn(13),
            phrase("Twelve").thenReturn(12),
            phrase("Eleven").thenReturn(11),
            phrase("Ten").thenReturn(10),
            phrase("Nine").thenReturn(9),
            phrase("Eight").thenReturn(8),
            phrase("Seven").thenReturn(7),
            phrase("Six").thenReturn(6),
            phrase("Five").thenReturn(5),
            phrase("Four").thenReturn(4),
            phrase("Three").thenReturn(3),
            phrase("Two").thenReturn(2),
            phrase("One").thenReturn(1));

    /// An integer written either as digits (`3`) or an English word
    /// number (`three`). Prefer this when oracle text accepts both.
    public static final Parser<Integer> NUMBER = anyOf(WORD_NUMBER, INTEGER);

    /// `+` or one of the two minus glyphs accepted by oracle text /
    /// Scryfall: ASCII hyphen `-` and U+2212 typographic minus.
    private static final CharPredicate MINUS_SIGN = CharPredicate.anyOf("-−");

    /// Signed integer — `+N`, `-N`, or `−N`. The sign token is
    /// consumed and folded into the result; only the combined signed
    /// value is returned.
    public static final Parser<Integer> SIGNED_INT = sequence(
            anyOf(one('+').thenReturn(1), one(MINUS_SIGN, "minus").thenReturn(-1)), INTEGER, (sign, num) -> sign * num);
}
