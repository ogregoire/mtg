package be.imgn.mtg.engine.ability.internal.parser;

import static be.imgn.mtg.parse.Parser.anyOf;
import static be.imgn.mtg.parse.Parser.digits;
import static be.imgn.mtg.parse.Parser.sequence;
import static be.imgn.mtg.parse.Parser.string;
import static be.imgn.mtg.parse.Parser.word;

import be.imgn.mtg.engine.ability.internal.parser.selector.Comparison;
import be.imgn.mtg.parse.Parser;

/// Common parser building blocks for oracle text parsing.
public final class CommonParsers {

    private CommonParsers() {}

    /// Parses an integer from digits.
    public static final Parser<Integer> INTEGER = digits().map(Integer::parseInt);

    /// Parses "or less" comparison.
    public static final Parser<Comparison> OR_LESS = string("or less").thenReturn(Comparison.LESS_OR_EQUAL);

    /// Parses "or greater" comparison.
    public static final Parser<Comparison> OR_GREATER = string("or greater").thenReturn(Comparison.GREATER_OR_EQUAL);

    /// Parses a comparison suffix (or less, or greater, or implicit equals).
    public static final Parser<Comparison> COMPARISON_SUFFIX = anyOf(OR_LESS, OR_GREATER);

    /// Parses integer with optional comparison: "3 or less", "5 or greater", or just "4" (equals).
    public static Parser<ComparisonValue> integerWithComparison() {
        return sequence(
                INTEGER,
                COMPARISON_SUFFIX.orElse(Comparison.EQUAL),
                (value, comparison) -> new ComparisonValue(comparison, value));
    }

    /// Parses a word number like "one", "two", etc.
    /// Descending order ensures longer words are tried before their prefixes (e.g., "thirteen" before "three").
    public static final Parser<Integer> WORD_NUMBER = anyOf(
            string("one hundred").thenReturn(100),
            word("ninety-nine").thenReturn(99),
            word("fifty").thenReturn(50),
            word("thirty").thenReturn(30),
            word("twenty-nine").thenReturn(29),
            word("twenty-eight").thenReturn(28),
            word("twenty-seven").thenReturn(27),
            word("twenty-six").thenReturn(26),
            word("twenty-five").thenReturn(25),
            word("twenty-four").thenReturn(24),
            word("twenty-three").thenReturn(23),
            word("twenty-two").thenReturn(22),
            word("twenty-one").thenReturn(21),
            word("twenty").thenReturn(20),
            word("nineteen").thenReturn(19),
            word("eighteen").thenReturn(18),
            word("seventeen").thenReturn(17),
            word("sixteen").thenReturn(16),
            word("fifteen").thenReturn(15),
            word("fourteen").thenReturn(14),
            word("thirteen").thenReturn(13),
            word("twelve").thenReturn(12),
            word("eleven").thenReturn(11),
            word("ten").thenReturn(10),
            word("nine").thenReturn(9),
            word("eight").thenReturn(8),
            word("seven").thenReturn(7),
            word("six").thenReturn(6),
            word("five").thenReturn(5),
            word("four").thenReturn(4),
            word("three").thenReturn(3),
            word("two").thenReturn(2),
            word("one").thenReturn(1));

    /// Holds a comparison operator and value together.
    public record ComparisonValue(Comparison comparison, int value) {}
}
