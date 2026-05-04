package be.imgn.mtg.engine.oracle2.parser.selector;

import static be.imgn.mtg.engine.oracle2.parser.AmountParser.AMOUNT;
import static be.imgn.mtg.engine.oracle2.parser.Parsers.phrase;
import static com.google.common.labs.parse.Parser.anyOf;

import com.google.common.labs.parse.Parser;

import be.imgn.mtg.engine.oracle2.domain.Quantifier;
import be.imgn.mtg.engine.oracle2.domain.StandardQuantifier;

/// Parses [Quantifier] — "select how many?". Splits across the
/// bare-word [StandardQuantifier] determiners ("all", "each", "every",
/// "no", "any number of") and any [be.imgn.mtg.engine.oracle2.domain.Amount]
/// expression (`AMOUNT` from `oracle2.parser.AmountParser`).
///
/// "all", "each", "every" all map to [StandardQuantifier#ALL] —
/// MTG resolution doesn't distinguish them ({@mtg.rule 109.5}).
public final class QuantifierParser {
    private QuantifierParser() {}

    /// Bare-word quantifier determiners. Order matters: "any number"
    /// must be tried before any other "any" form (none exist today,
    /// but the precedence is documented for future arms).
    public static final Parser<StandardQuantifier> STANDARD = anyOf(
            phrase("Any number of").thenReturn(StandardQuantifier.ANY_NUMBER),
            phrase("[All|Each|Every]").thenReturn(StandardQuantifier.ALL),
            phrase("No").thenReturn(StandardQuantifier.NONE));

    /// Top-level [Quantifier] entry. Bare-word determiners are tried
    /// before [AmountParser#AMOUNT] so "any number of" wins over the
    /// numeric atoms (specifically: "any number of" must precede "no",
    /// which is consumed by [#STANDARD]; numeric forms come last).
    public static final Parser<Quantifier> QUANTIFIER = Parser.anyOf(STANDARD, AMOUNT);
}
