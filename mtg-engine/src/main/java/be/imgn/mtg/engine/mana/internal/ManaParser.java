package be.imgn.mtg.engine.mana.internal;

import static be.imgn.mtg.parse.Parser.anyOf;
import static be.imgn.mtg.parse.Parser.consecutive;
import static be.imgn.mtg.parse.Parser.sequence;
import static be.imgn.mtg.parse.Parser.single;
import static be.imgn.mtg.parse.Parser.string;
import static be.imgn.mtg.parse.Parser.word;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import be.imgn.mtg.engine.ability.internal.parser.CommonParsers;
import be.imgn.mtg.engine.ability.internal.parser.selector.Amount;
import be.imgn.mtg.engine.mana.AddExactManaEffect;
import be.imgn.mtg.engine.mana.AddManaCombinationEffect;
import be.imgn.mtg.engine.mana.AddManaEffect;
import be.imgn.mtg.engine.mana.AddManaSelectionEffect;
import be.imgn.mtg.engine.mana.ManaType;
import be.imgn.mtg.parse.CharPredicate;
import be.imgn.mtg.parse.Parser;

/// Parser for mana effects in oracle text.
public final class ManaParser {

    private ManaParser() {}

    /// Matches color and special mana symbols: W, U, B, R, G, C, X.
    private static final CharPredicate MANA_LETTER =
            CharPredicate.is('W').or('U').or('B').or('R').or('G').or('C').or('X');

    /// Matches colored mana symbols only: W, U, B, R, G.
    private static final CharPredicate COLORED_MANA_LETTER =
            CharPredicate.is('W').or('U').or('B').or('R').or('G');

    /// Matches producible mana symbols: W, U, B, R, G, C (colored + colorless).
    private static final CharPredicate PRODUCIBLE_MANA_LETTER =
            CharPredicate.is('W').or('U').or('B').or('R').or('G').or('C');

    /// Parses the content inside a mana symbol: a letter (W, U, B, R, G, C, X) or a number (0-16).
    private static final Parser<String> MANA_CONTENT = anyOf(
            single(MANA_LETTER, "mana letter").map(String::valueOf),
            consecutive(CharPredicate.ASCII_DIGIT, "mana number"));

    /// Parses a single mana symbol like `{G}`, `{1}`, or `{16}`.
    private static final Parser<String> SINGLE_MANA_SYMBOL =
            MANA_CONTENT.immediatelyBetween("{", "}").map(c -> "{" + c + "}");

    /// Parses one or more mana symbols like "{G}{G}" or "{R}".
    private static final Parser<String> MANA_SYMBOLS = SINGLE_MANA_SYMBOL.atLeastOnce(Collectors.joining());

    /// Parses a single producible mana letter and converts to ManaType.
    private static final Parser<ManaType> PRODUCIBLE_MANA_TYPE =
            single(PRODUCIBLE_MANA_LETTER, "mana letter").map(ManaParser::charToManaType);

    /// Parses a single colored mana letter and converts to ManaType.Colored.
    private static final Parser<ManaType.Colored> COLORED_MANA_TYPE =
            single(COLORED_MANA_LETTER, "colored mana letter").map(ManaParser::charToColoredManaType);

    /// Parses one or more producible mana symbols like "{G}{G}", "{R}", or "{C}{C}" and converts to a list of
    // ManaTypes.
    private static final Parser<List<ManaType>> PRODUCIBLE_MANA_TYPES =
            PRODUCIBLE_MANA_TYPE.immediatelyBetween("{", "}").atLeastOnce();

    /// Parses a single colored mana symbol like "{G}" or "{R}" and converts to ManaType.Colored.
    private static final Parser<ManaType.Colored> SINGLE_COLORED_MANA_SYMBOL =
            COLORED_MANA_TYPE.immediatelyBetween("{", "}");

    /// Parses "Add {G}." or "Add {G}{G}."
    ///
    /// Pattern: "Add" mana ["."]
    private static final Parser<AddExactManaEffect> ADD_EXACT_MANA =
            word("Add").then(MANA_SYMBOLS).map(AddExactManaEffect::new).optionallyFollowedBy(".");

    /// Parses an amount: word number or X.
    private static final Parser<Amount> MANA_AMOUNT =
            anyOf(word("X").thenReturn((Amount) new Amount.XValue()), CommonParsers.WORD_NUMBER.map(Amount.Exact::new));

    /// Parses "{R} and/or {G}" pattern (two types).
    private static final Parser<Set<ManaType.Colored>> AND_OR_TWO_TYPES = sequence(
            SINGLE_COLORED_MANA_SYMBOL,
            string("and/or").then(SINGLE_COLORED_MANA_SYMBOL),
            ManaParser::twoColoredTypesSet);

    /// Parses "{W}, {U}, and/or {B}" pattern (three or more types with Oxford comma).
    private static final Parser<Set<ManaType.Colored>> AND_OR_MORE_TYPES = sequence(
            sequence(
                    SINGLE_COLORED_MANA_SYMBOL,
                    string(", ").then(SINGLE_COLORED_MANA_SYMBOL).atLeastOnce(),
                    ManaParser::combineFirstColoredType),
            string(", and/or").then(SINGLE_COLORED_MANA_SYMBOL),
            ManaParser::appendLastColoredType);

    /// Parses any "and/or" mana type list.
    private static final Parser<Set<ManaType.Colored>> AND_OR_MANA_TYPES = anyOf(AND_OR_MORE_TYPES, AND_OR_TWO_TYPES);

    /// Parses "Add one mana of any color." or "Add X mana in any combination of colors."
    ///
    /// Pattern: "Add" amount "mana" ("of any color" | "in any combination of colors") ["."]
    private static final Parser<AddManaCombinationEffect> ADD_MANA_COMBINATION_ANY_COLOR = word("Add")
            .then(MANA_AMOUNT)
            .followedBy(anyOf(string("mana of any color"), string("mana in any combination of colors")))
            .map(AddManaCombinationEffect::anyColor)
            .optionallyFollowedBy(".");

    /// Parses "Add three mana in any combination of {R} and/or {G}."
    ///
    /// Pattern: "Add" amount "mana in any combination of" mana_list ["."]
    private static final Parser<AddManaCombinationEffect> ADD_MANA_COMBINATION_OF_SET = word("Add")
            .then(sequence(
                    MANA_AMOUNT,
                    string("mana in any combination of").then(AND_OR_MANA_TYPES),
                    AddManaCombinationEffect::new))
            .optionallyFollowedBy(".");

    /// Parses "Add four mana of any one color." or "Add X mana of any one color."
    ///
    /// Pattern: "Add" amount "mana of any one color" ["."]
    private static final Parser<AddManaSelectionEffect> ADD_MANA_SELECTION_ANY_ONE_COLOR = word("Add")
            .then(MANA_AMOUNT)
            .followedBy(string("mana of any one color"))
            .map(AddManaSelectionEffect::anyOneColor)
            .optionallyFollowedBy(".");

    /// Parses "Add {R} or {G}."
    ///
    /// Pattern: "Add" mana "or" mana ["."]
    private static final Parser<AddManaSelectionEffect> ADD_MANA_SELECTION_TWO_OPTIONS = word("Add")
            .then(sequence(PRODUCIBLE_MANA_TYPES, word("or").then(PRODUCIBLE_MANA_TYPES), ManaParser::twoOptions))
            .map(AddManaSelectionEffect::new)
            .optionallyFollowedBy(".");

    /// Parses "Add {R}{R}, {R}{G}, or {G}{G}." or "Add {W}, {U}, {B}, or {C}{C}."
    ///
    /// Pattern: "Add" mana ("," mana)+ ", or" mana ["."]
    private static final Parser<AddManaSelectionEffect> ADD_MANA_SELECTION_MORE_OPTIONS = word("Add")
            .then(sequence(
                    sequence(
                            PRODUCIBLE_MANA_TYPES,
                            string(", ").then(PRODUCIBLE_MANA_TYPES).atLeastOnce(),
                            ManaParser::combineFirst),
                    string(", or").then(PRODUCIBLE_MANA_TYPES),
                    ManaParser::appendLast))
            .map(AddManaSelectionEffect::new)
            .optionallyFollowedBy(".");

    /// Parses "Add {U} or {B}." or "Add {R}{R}, {R}{G}, or {G}{G}."
    private static final Parser<AddManaSelectionEffect> ADD_MANA_SELECTION =
            anyOf(ADD_MANA_SELECTION_MORE_OPTIONS, ADD_MANA_SELECTION_TWO_OPTIONS);

    /// Unified parser for all "Add mana" effects.
    ///
    /// Parses:
    /// - "Add {G}." or "Add {G}{G}." → AddExactManaEffect
    /// - "Add {U} or {B}." or "Add {R}{R}, {R}{G}, or {G}{G}." → AddManaFromSelectionEffect
    /// - "Add two mana of any one color." → AddManaFromSelectionEffect
    /// - "Add one mana of any color." → AddManaOfAnyCombinationEffect (all colors)
    /// - "Add three mana in any combination of {R} and/or {G}." → AddManaOfAnyCombinationEffect (specific colors)
    public static final Parser<AddManaEffect> ADD_MANA = anyOf(
            ADD_MANA_SELECTION,
            ADD_MANA_SELECTION_ANY_ONE_COLOR,
            ADD_MANA_COMBINATION_OF_SET,
            ADD_MANA_COMBINATION_ANY_COLOR,
            ADD_EXACT_MANA);

    private static List<List<ManaType>> twoOptions(List<ManaType> first, List<ManaType> second) {
        return List.of(first, second);
    }

    private static List<List<ManaType>> combineFirst(List<ManaType> first, List<List<ManaType>> rest) {
        var options = new ArrayList<List<ManaType>>();
        options.add(first);
        options.addAll(rest);
        return options;
    }

    private static List<List<ManaType>> appendLast(List<List<ManaType>> options, List<ManaType> last) {
        options.add(last);
        return options;
    }

    private static Set<ManaType.Colored> twoColoredTypesSet(ManaType.Colored first, ManaType.Colored second) {
        return EnumSet.of(first, second);
    }

    private static Set<ManaType.Colored> combineFirstColoredType(ManaType.Colored first, List<ManaType.Colored> rest) {
        var types = EnumSet.of(first);
        types.addAll(rest);
        return types;
    }

    private static Set<ManaType.Colored> appendLastColoredType(Set<ManaType.Colored> types, ManaType.Colored last) {
        types.add(last);
        return types;
    }

    /// Converts a mana letter character to its corresponding ManaType.
    private static ManaType charToManaType(char c) {
        return switch (c) {
            case 'W' -> ManaType.WHITE;
            case 'U' -> ManaType.BLUE;
            case 'B' -> ManaType.BLACK;
            case 'R' -> ManaType.RED;
            case 'G' -> ManaType.GREEN;
            case 'C' -> ManaType.COLORLESS;
            default -> throw new IllegalArgumentException("Unknown mana letter: " + c);
        };
    }

    /// Converts a colored mana letter character to its corresponding ManaType.Colored.
    private static ManaType.Colored charToColoredManaType(char c) {
        return switch (c) {
            case 'W' -> ManaType.Colored.WHITE;
            case 'U' -> ManaType.Colored.BLUE;
            case 'B' -> ManaType.Colored.BLACK;
            case 'R' -> ManaType.Colored.RED;
            case 'G' -> ManaType.Colored.GREEN;
            default -> throw new IllegalArgumentException("Unknown colored mana letter: " + c);
        };
    }
}
