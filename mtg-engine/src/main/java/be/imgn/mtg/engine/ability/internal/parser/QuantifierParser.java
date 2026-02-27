package be.imgn.mtg.engine.ability.internal.parser;

import static com.google.common.labs.parse.Parser.anyOf;
import static com.google.common.labs.parse.Parser.sequence;
import static com.google.common.labs.parse.Parser.word;

import com.google.common.labs.parse.Parser;

import be.imgn.mtg.engine.selector.Quantifier;

/// Parser for quantifiers in oracle text.
public final class QuantifierParser {

    private QuantifierParser() {}

    /// Parses "all" quantifier.
    public static final Parser<Quantifier> ALL = word("all").thenReturn(new Quantifier.All());

    /// Parses "each" quantifier.
    public static final Parser<Quantifier> EACH = word("each").thenReturn(new Quantifier.Each());

    /// Parses "another" quantifier.
    public static final Parser<Quantifier> ANOTHER = word("another").thenReturn(new Quantifier.Another());

    /// Parses "any number of" quantifier.
    public static final Parser<Quantifier> ANY_NUMBER =
            sequence(word("any"), word("number"), word("of"), (a, b, c) -> new Quantifier.Any());

    /// Parses standalone "any" quantifier (for "any target" patterns).
    public static final Parser<Quantifier> JUST_ANY = word("any").thenReturn(new Quantifier.One());

    /// Parses "up to N" quantifier.
    public static final Parser<Quantifier> UP_TO =
            sequence(word("up"), word("to"), CommonParsers.WORD_NUMBER, (up, to, n) -> new Quantifier.UpTo(n));

    /// Parses word-based count quantifiers.
    public static final Parser<Quantifier> WORD_COUNT = CommonParsers.WORD_NUMBER.map(Quantifier.Count::new);

    /// Parses a digit-based count.
    public static final Parser<Quantifier> DIGIT_COUNT = CommonParsers.INTEGER.map(Quantifier.Count::new);

    /// Parses "a" or "an" as One quantifier (implicit single).
    public static final Parser<Quantifier> A_AN = anyOf(word("a"), word("an")).thenReturn(new Quantifier.One());

    /// Parses any explicit quantifier.
    /// Note: ANY_NUMBER must come before JUST_ANY to match "any number of" first.
    public static final Parser<Quantifier> EXPLICIT_QUANTIFIER =
            anyOf(ALL, EACH, ANOTHER, ANY_NUMBER, JUST_ANY, UP_TO, WORD_COUNT, A_AN);

    /// Parses a quantifier, defaulting to One if not present.
    public static final Parser<Quantifier>.OrEmpty QUANTIFIER = EXPLICIT_QUANTIFIER.orElse(new Quantifier.One());
}
