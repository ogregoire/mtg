package be.imgn.mtg.engine.ability.internal.parser;

import static be.imgn.mtg.parse.Parser.anyOf;
import static be.imgn.mtg.parse.Parser.string;
import static be.imgn.mtg.parse.Parser.word;

import be.imgn.mtg.engine.ability.internal.parser.selector.Amount;
import be.imgn.mtg.parse.Parser;

/// Parser for numeric amounts in oracle text.
public final class AmountParser {

    private AmountParser() {}

    /// Parses X value.
    public static final Parser<Amount> X_VALUE = word("X").thenReturn(new Amount.XValue());

    /// Parses a numeric amount (digit or word number).
    public static final Parser<Amount> NUMERIC =
            anyOf(CommonParsers.INTEGER, CommonParsers.WORD_NUMBER).map(Amount.Exact::new);

    /// Parses "that much" or similar variable references.
    public static final Parser<Amount> VARIABLE = anyOf(
                    string("that much").thenReturn("that much"),
                    string("that many").thenReturn("that many"))
            .map(Amount.Variable::new);

    /// Parses any amount.
    public static final Parser<Amount> AMOUNT = anyOf(X_VALUE, VARIABLE, NUMERIC);
}
