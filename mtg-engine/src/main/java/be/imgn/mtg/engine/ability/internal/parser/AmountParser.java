package be.imgn.mtg.engine.ability.internal.parser;

import static com.google.common.labs.parse.Parser.anyOf;
import static com.google.common.labs.parse.Parser.string;
import static com.google.common.labs.parse.Parser.word;

import com.google.common.labs.parse.Parser;

import be.imgn.mtg.engine.ability.internal.parser.selector.Amount;

/// Parser for numeric amounts in oracle text.
public final class AmountParser {

    private AmountParser() {}

    /// Parses X value.
    public static final Parser<Amount> X_VALUE = word("X").thenReturn(Amount.X);

    /// Parses a numeric amount (digit or word number).
    public static final Parser<Amount> NUMERIC =
            anyOf(CommonParsers.INTEGER, CommonParsers.WORD_NUMBER).map(Amount.Exact::new);

    /// Parses "that much" or similar references.
    public static final Parser<Amount> REFERENCE = anyOf(
                    string("that much").thenReturn("that much"),
                    string("that many").thenReturn("that many"))
            .map(Amount.Reference::new);

    /// Parses any amount.
    public static final Parser<Amount> AMOUNT = anyOf(X_VALUE, REFERENCE, NUMERIC);
}
