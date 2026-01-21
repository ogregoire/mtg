package be.imgn.mtg.engine.ability.internal.parser;

import static be.imgn.mtg.parse.Parser.anyOf;
import static be.imgn.mtg.parse.Parser.consecutive;
import static be.imgn.mtg.parse.Parser.single;
import static be.imgn.mtg.parse.Parser.string;
import static be.imgn.mtg.parse.Parser.word;

import java.util.stream.Collectors;

import be.imgn.mtg.engine.ability.internal.parser.effect.AddManaEffect;
import be.imgn.mtg.engine.mana.AddManaOfAnyColorCombination;
import be.imgn.mtg.engine.mana.AddManaOfAnyOneColor;
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

    /// Parses a number word like "one", "two", "three", etc.
    private static final Parser<Integer> NUMBER_WORD = anyOf(
            word("one").thenReturn(1),
            word("two").thenReturn(2),
            word("three").thenReturn(3),
            word("four").thenReturn(4),
            word("five").thenReturn(5),
            word("six").thenReturn(6),
            word("seven").thenReturn(7),
            word("eight").thenReturn(8),
            word("nine").thenReturn(9),
            word("ten").thenReturn(10));

    /// Parses "Add one mana of any color." or "Add X mana in any combination of colors."
    ///
    /// Pattern: "Add" number "mana" ("of any color" | "in any combination of colors") ["."]
    public static final Parser<AddManaOfAnyColorCombination> ADD_MANA_ANY_COLOR_COMBINATION = word("Add")
            .then(NUMBER_WORD)
            .followedBy(word("mana"))
            .followedBy(anyOf(
                    word("of").then(word("any")).then(word("color")),
                    word("in")
                            .then(word("any"))
                            .then(word("combination"))
                            .then(word("of"))
                            .then(word("colors"))))
            .map(AddManaOfAnyColorCombination::new)
            .optionallyFollowedBy(".");

    /// Parses "Add four mana of any one color."
    ///
    /// Pattern: "Add" number "mana of any one color" ["."]
    public static final Parser<AddManaOfAnyOneColor> ADD_MANA_ANY_ONE_COLOR = word("Add")
            .then(NUMBER_WORD)
            .followedBy(word("mana"))
            .followedBy(word("of"))
            .followedBy(word("any"))
            .followedBy(word("one"))
            .followedBy(word("color"))
            .map(AddManaOfAnyOneColor::new)
            .optionallyFollowedBy(".");
}
