package be.imgn.mtg.engine.ability.internal.parser;

import static be.imgn.mtg.parse.Parser.anyOf;
import static be.imgn.mtg.parse.Parser.consecutive;
import static be.imgn.mtg.parse.Parser.single;
import static be.imgn.mtg.parse.Parser.string;
import static be.imgn.mtg.parse.Parser.word;

import java.util.stream.Collectors;

import be.imgn.mtg.engine.ability.internal.parser.effect.AddManaEffect;
import be.imgn.mtg.parse.CharPredicate;
import be.imgn.mtg.parse.Parser;

/// Parser for mana effects in oracle text.
public final class ManaParser {

    private ManaParser() {}

    /// Matches color and special mana symbols: W, U, B, R, G, C, X.
    private static final CharPredicate MANA_LETTER =
            CharPredicate.is('W').or('U').or('B').or('R').or('G').or('C').or('X');

    /// Parses the content inside a mana symbol: a letter (W, U, B, R, G, C, X) or a number (0-16).
    private static final Parser<String> MANA_CONTENT = anyOf(
            single(MANA_LETTER, "mana letter").map(String::valueOf),
            consecutive(CharPredicate.ASCII_DIGIT, "mana number"));

    /// Parses a single mana symbol like `{G}`, `{1}`, or `{16}`.
    private static final Parser<String> SINGLE_MANA_SYMBOL =
            string("{").then(MANA_CONTENT).followedBy(string("}")).map(c -> "{" + c + "}");

    /// Parses one or more mana symbols like "{G}{G}" or "{R}".
    private static final Parser<String> MANA_SYMBOLS = SINGLE_MANA_SYMBOL.atLeastOnce(Collectors.joining());

    /// Parses "Add {G}." or "Add {G}{G}."
    ///
    /// Pattern: "Add" mana ["."]
    public static final Parser<AddManaEffect> ADD_MANA_EFFECT =
            word("Add").then(MANA_SYMBOLS).map(AddManaEffect::new).optionallyFollowedBy(".");
}
