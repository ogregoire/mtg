package be.imgn.mtg.engine.mana.internal;

import static com.google.common.labs.parse.Parser.anyOf;
import static com.google.common.labs.parse.Parser.digits;
import static com.google.common.labs.parse.Parser.one;
import static com.google.common.labs.parse.Parser.sequence;
import static com.google.common.labs.parse.Parser.string;
import static com.google.common.labs.parse.Parser.word;
import static java.util.stream.Collectors.toCollection;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import com.google.common.labs.parse.Parser;
import com.google.mu.util.CharPredicate;

import be.imgn.mtg.engine.ability.internal.parser.CommonParsers;
import be.imgn.mtg.engine.ability.internal.parser.selector.Amount;
import be.imgn.mtg.engine.mana.AddManaEffect;
import be.imgn.mtg.engine.mana.ManaCost;
import be.imgn.mtg.engine.mana.ManaSymbol;
import be.imgn.mtg.engine.mana.ManaType;

/// Unified parser for mana-related text: mana costs and "Add mana" effects.
public final class ManaParser {

    private ManaParser() {}

    // ========================================
    // Character predicates
    // ========================================

    /// Matches W, U, B, R, or G.
    private static final CharPredicate COLORED_LETTER = CharPredicate.anyOf("WUBRG");

    /// Matches W, U, B, R, G, or C.
    private static final CharPredicate PRODUCIBLE_LETTER = CharPredicate.anyOf("WUBRGC");

    // ========================================
    // Mana cost symbol content parsers
    // ========================================

    /// Parses "W" to a colored mana type (content inside braces).
    private static final Parser<ManaType.Colored> COLORED_CONTENT =
            one(COLORED_LETTER, "colored mana letter").map(ManaParser::charToColoredManaType);

    /// Parses "C" (content inside braces).
    private static final Parser<String> COLORLESS_CONTENT = string("C");

    /// Parses "X" (content inside braces).
    private static final Parser<String> VARIABLE_CONTENT = string("X");

    /// Parses "S" (content inside braces).
    private static final Parser<String> SNOW_CONTENT = string("S");

    /// Parses digits like "0", "1", "2" (content inside braces).
    private static final Parser<Integer> GENERIC_CONTENT = digits().map(Integer::parseInt);

    /// Parses "W/P" to the colored mana type (content inside braces).
    private static final Parser<ManaType.Colored> PHYREXIAN_CONTENT = COLORED_CONTENT.followedBy("/P");

    /// Parses "W/U" to a color pair (content inside braces). Only valid hybrid pairs are accepted.
    private static final Parser<ColorPair> HYBRID_CONTENT =
            one(COLORED_LETTER, "hybrid first color").followedBy("/").flatMap(ManaParser::hybridPartnerParser);

    /// Parses "W/U/P" to a color pair (content inside braces). Only valid hybrid pairs are accepted.
    private static final Parser<ColorPair> HYBRID_PHYREXIAN_CONTENT = one(COLORED_LETTER, "hybrid first color")
            .followedBy("/")
            .flatMap(ManaParser::hybridPartnerParser)
            .followedBy("/P");

    /// Parses "2/W" to the colored mana type (content inside braces).
    private static final Parser<ManaType.Colored> MONO_COLOR_HYBRID_CONTENT =
            string("2/").then(COLORED_CONTENT);

    /// Parses "C/W" to the colored mana type (content inside braces).
    private static final Parser<ManaType.Colored> COLORLESS_HYBRID_CONTENT =
            string("C/").then(COLORED_CONTENT);

    /// Parses a single mana symbol like "{W}" or "{2/W}".
    private static final Parser<ManaSymbol> MANA_SYMBOL = anyOf(
                    HYBRID_PHYREXIAN_CONTENT.map(ManaParser::colorsToHybridPhyrexian),
                    PHYREXIAN_CONTENT.map(ManaParser::coloredToPhyrexian),
                    HYBRID_CONTENT.map(ManaParser::colorsToHybrid),
                    MONO_COLOR_HYBRID_CONTENT.map(ManaParser::coloredToMonoColorHybrid),
                    COLORLESS_HYBRID_CONTENT.map(ManaParser::coloredToColorlessHybrid),
                    COLORED_CONTENT.map(ManaSymbol.Colored::fromManaType),
                    COLORLESS_CONTENT.thenReturn(ManaSymbol.Colorless.COLORLESS),
                    VARIABLE_CONTENT.thenReturn(ManaSymbol.Variable.X),
                    SNOW_CONTENT.thenReturn(ManaSymbol.Snow.SNOW),
                    GENERIC_CONTENT.map(ManaSymbol.Generic::new))
            .immediatelyBetween("{", "}");

    // ========================================
    // Mana type parsers (for add mana effects)
    // ========================================

    /// Parses "{W}", "{U}", "{B}", "{R}", or "{G}" to a colored mana type.
    private static final Parser<ManaType.Colored> COLORED_MANA_TYPE = COLORED_CONTENT.immediatelyBetween("{", "}");

    /// Parses "{W}", "{U}", "{B}", "{R}", "{G}", or "{C}" to a mana type.
    private static final Parser<ManaType> PRODUCIBLE_MANA_TYPE =
            one(PRODUCIBLE_LETTER, "mana letter").immediatelyBetween("{", "}").map(ManaParser::charToManaType);

    /// Parses one or more mana symbols, e.g. "{G}" or "{R}{G}".
    private static final Parser<List<ManaType>> PRODUCIBLE_MANA_TYPES = PRODUCIBLE_MANA_TYPE.atLeastOnce();

    // ========================================
    // Add mana effect parsers
    // ========================================

    /// Parses "X" or a word number like "one", "two", "three".
    private static final Parser<Amount> AMOUNT =
            anyOf(word("X").thenReturn(Amount.X), CommonParsers.WORD_NUMBER.map(Amount.Exact::new));

    /// Parses "{R} and/or {G}".
    private static final Parser<Set<ManaType.Colored>> COMBINATION_TWO_COLORS =
            sequence(COLORED_MANA_TYPE, string("and/or").then(COLORED_MANA_TYPE), EnumSet::of);

    /// Parses "{W}, {U}, and/or {B}".
    private static final Parser<Set<ManaType.Colored>> COMBINATION_MORE_COLORS = sequence(
            COLORED_MANA_TYPE.atLeastOnceDelimitedBy(", ", toCollection(() -> EnumSet.noneOf(ManaType.Colored.class))),
            string(", and/or").then(COLORED_MANA_TYPE),
            ManaParser::addToSet);

    /// Parses "{R} and/or {G}" or "{W}, {U}, and/or {B}".
    private static final Parser<Set<ManaType.Colored>> COMBINATION_COLORS =
            anyOf(COMBINATION_MORE_COLORS, COMBINATION_TWO_COLORS);

    /// Parses "{G}" or "{G}{G}".
    private static final Parser<AddManaEffect> EXACT_EFFECT = PRODUCIBLE_MANA_TYPES.map(AddManaEffect.Exact::new);

    /// Parses "X {G}".
    private static final Parser<AddManaEffect> VARIABLE_EFFECT =
            word("X").then(PRODUCIBLE_MANA_TYPE).map(AddManaEffect.Variable::new);

    /// Parses "{R} or {G}".
    private static final Parser<AddManaEffect> SELECTION_TWO_EFFECT = sequence(
                    PRODUCIBLE_MANA_TYPES, word("or").then(PRODUCIBLE_MANA_TYPES), List::of)
            .map(AddManaEffect.Selection::new);

    /// Parses "{R}{R}, {R}{G}, or {G}{G}".
    private static final Parser<AddManaEffect> SELECTION_MANY_EFFECT = sequence(
                    PRODUCIBLE_MANA_TYPES.atLeastOnceDelimitedBy(", ", toCollection(ArrayList::new)),
                    string(", or").then(PRODUCIBLE_MANA_TYPES),
                    ManaParser::append)
            .map(AddManaEffect.Selection::new);

    /// Parses "{U} or {B}" or "{R}{R}, {R}{G}, or {G}{G}".
    private static final Parser<AddManaEffect> SELECTION_EFFECT = anyOf(SELECTION_MANY_EFFECT, SELECTION_TWO_EFFECT);

    /// Parses "two mana of any one color".
    private static final Parser<AddManaEffect> SELECTION_ANY_ONE_COLOR_EFFECT =
            AMOUNT.followedBy(string("mana of any one color")).map(AddManaEffect.Selection::anyOneColor);

    /// Parses "one mana of any color" or "X mana in any combination of colors".
    private static final Parser<AddManaEffect> COMBINATION_ANY_COLOR_EFFECT = AMOUNT.followedBy(
                    anyOf(string("mana of any color"), string("mana in any combination of colors")))
            .map(AddManaEffect.Combination::anyColor);

    /// Parses "three mana in any combination of {R} and/or {G}".
    private static final Parser<AddManaEffect> COMBINATION_OF_COLORS_EFFECT = sequence(
            AMOUNT, string("mana in any combination of").then(COMBINATION_COLORS), AddManaEffect.Combination::new);

    // ========================================
    // Public API
    // ========================================

    /// Parses a mana cost like "{2}{W}{W}" or "{0}".
    public static final Parser<ManaCost> MANA_COST = anyOf(
            string("{0}").thenReturn(DefaultManaCost.EMPTY),
            MANA_SYMBOL.atLeastOnce().<ManaCost>map(DefaultManaCost::new));

    /// Parses an "Add mana" effect from oracle text.
    ///
    /// Parses:
    /// - "Add {G}." or "Add {G}{G}." → Exact
    /// - "Add X {G}." → Variable
    /// - "Add {U} or {B}." or "Add {R}{R}, {R}{G}, or {G}{G}." → Selection
    /// - "Add two mana of any one color." → Selection
    /// - "Add one mana of any color." → Combination
    /// - "Add three mana in any combination of {R} and/or {G}." → Combination
    public static final Parser<AddManaEffect> ADD_MANA_EFFECT = word("Add")
            .then(anyOf(
                    SELECTION_EFFECT,
                    SELECTION_ANY_ONE_COLOR_EFFECT,
                    COMBINATION_OF_COLORS_EFFECT,
                    COMBINATION_ANY_COLOR_EFFECT,
                    VARIABLE_EFFECT,
                    EXACT_EFFECT))
            .optionallyFollowedBy(".");

    // ========================================
    // Helper methods
    // ========================================

    private static <T> List<T> append(List<T> list, T last) {
        list.add(last);
        return list;
    }

    private static Set<ManaType.Colored> addToSet(Set<ManaType.Colored> set, ManaType.Colored element) {
        set.add(element);
        return set;
    }

    private static ManaType charToManaType(char c) {
        return switch (c) {
            case 'W' -> ManaType.WHITE;
            case 'U' -> ManaType.BLUE;
            case 'B' -> ManaType.BLACK;
            case 'R' -> ManaType.RED;
            case 'G' -> ManaType.GREEN;
            case 'C' -> ManaType.COLORLESS;
            default -> throw new AssertionError("Unreachable: " + c);
        };
    }

    private static ManaType.Colored charToColoredManaType(char c) {
        return switch (c) {
            case 'W' -> ManaType.WHITE;
            case 'U' -> ManaType.BLUE;
            case 'B' -> ManaType.BLACK;
            case 'R' -> ManaType.RED;
            case 'G' -> ManaType.GREEN;
            default -> throw new AssertionError("Unreachable: " + c);
        };
    }

    private static ManaSymbol.Phyrexian coloredToPhyrexian(ManaType.Colored color) {
        return switch (color) {
            case WHITE -> ManaSymbol.Phyrexian.WHITE_PHYREXIAN;
            case BLUE -> ManaSymbol.Phyrexian.BLUE_PHYREXIAN;
            case BLACK -> ManaSymbol.Phyrexian.BLACK_PHYREXIAN;
            case RED -> ManaSymbol.Phyrexian.RED_PHYREXIAN;
            case GREEN -> ManaSymbol.Phyrexian.GREEN_PHYREXIAN;
        };
    }

    private static Parser<ColorPair> hybridPartnerParser(char first) {
        var firstColor = charToColoredManaType(first);
        var partners =
                switch (first) {
                    case 'W' -> "UB";
                    case 'U' -> "BR";
                    case 'B' -> "RG";
                    case 'R' -> "GW";
                    case 'G' -> "WU";
                    default -> throw new AssertionError("Unreachable: " + first);
                };
        return one(CharPredicate.anyOf(partners), first + " hybrid partner")
                .map(c -> new ColorPair(firstColor, charToColoredManaType(c)));
    }

    private static ManaSymbol.Hybrid colorsToHybrid(ColorPair colors) {
        return switch (colors.first) {
            case WHITE ->
                colors.second == ManaType.Colored.BLUE ? ManaSymbol.Hybrid.WHITE_BLUE : ManaSymbol.Hybrid.WHITE_BLACK;
            case BLUE ->
                colors.second == ManaType.Colored.BLACK ? ManaSymbol.Hybrid.BLUE_BLACK : ManaSymbol.Hybrid.BLUE_RED;
            case BLACK ->
                colors.second == ManaType.Colored.RED ? ManaSymbol.Hybrid.BLACK_RED : ManaSymbol.Hybrid.BLACK_GREEN;
            case RED ->
                colors.second == ManaType.Colored.GREEN ? ManaSymbol.Hybrid.RED_GREEN : ManaSymbol.Hybrid.RED_WHITE;
            case GREEN ->
                colors.second == ManaType.Colored.WHITE ? ManaSymbol.Hybrid.GREEN_WHITE : ManaSymbol.Hybrid.GREEN_BLUE;
        };
    }

    private static ManaSymbol.HybridPhyrexian colorsToHybridPhyrexian(ColorPair colors) {
        return switch (colors.first) {
            case WHITE ->
                colors.second == ManaType.Colored.BLUE
                        ? ManaSymbol.HybridPhyrexian.WHITE_BLUE_PHYREXIAN
                        : ManaSymbol.HybridPhyrexian.WHITE_BLACK_PHYREXIAN;
            case BLUE ->
                colors.second == ManaType.Colored.BLACK
                        ? ManaSymbol.HybridPhyrexian.BLUE_BLACK_PHYREXIAN
                        : ManaSymbol.HybridPhyrexian.BLUE_RED_PHYREXIAN;
            case BLACK ->
                colors.second == ManaType.Colored.RED
                        ? ManaSymbol.HybridPhyrexian.BLACK_RED_PHYREXIAN
                        : ManaSymbol.HybridPhyrexian.BLACK_GREEN_PHYREXIAN;
            case RED ->
                colors.second == ManaType.Colored.GREEN
                        ? ManaSymbol.HybridPhyrexian.RED_GREEN_PHYREXIAN
                        : ManaSymbol.HybridPhyrexian.RED_WHITE_PHYREXIAN;
            case GREEN ->
                colors.second == ManaType.Colored.WHITE
                        ? ManaSymbol.HybridPhyrexian.GREEN_WHITE_PHYREXIAN
                        : ManaSymbol.HybridPhyrexian.GREEN_BLUE_PHYREXIAN;
        };
    }

    private static ManaSymbol.MonoColorHybrid coloredToMonoColorHybrid(ManaType.Colored color) {
        return switch (color) {
            case WHITE -> ManaSymbol.MonoColorHybrid.TWO_WHITE;
            case BLUE -> ManaSymbol.MonoColorHybrid.TWO_BLUE;
            case BLACK -> ManaSymbol.MonoColorHybrid.TWO_BLACK;
            case RED -> ManaSymbol.MonoColorHybrid.TWO_RED;
            case GREEN -> ManaSymbol.MonoColorHybrid.TWO_GREEN;
        };
    }

    private static ManaSymbol.ColorlessHybrid coloredToColorlessHybrid(ManaType.Colored color) {
        return switch (color) {
            case WHITE -> ManaSymbol.ColorlessHybrid.COLORLESS_WHITE;
            case BLUE -> ManaSymbol.ColorlessHybrid.COLORLESS_BLUE;
            case BLACK -> ManaSymbol.ColorlessHybrid.COLORLESS_BLACK;
            case RED -> ManaSymbol.ColorlessHybrid.COLORLESS_RED;
            case GREEN -> ManaSymbol.ColorlessHybrid.COLORLESS_GREEN;
        };
    }

    /// Helper record for hybrid color pairs.
    private record ColorPair(ManaType.Colored first, ManaType.Colored second) {}
}
