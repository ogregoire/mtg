package be.imgn.mtg.engine.oracle.parser;

import static be.imgn.mtg.engine.oracle.parser.SelectorParsers.CARD_TYPE;
import static be.imgn.mtg.engine.oracle.parser.SelectorParsers.COLOR;
import static be.imgn.mtg.engine.oracle.parser.SelectorParsers.PT_VALUE;
import static be.imgn.mtg.engine.oracle.parser.SelectorParsers.SUBTYPE;
import static be.imgn.mtg.engine.oracle.parser.Words.phrase;
import static com.google.common.labs.parse.Parser.anyOf;
import static com.google.common.labs.parse.Parser.or;
import static com.google.common.labs.parse.Parser.sequence;
import static com.google.common.labs.parse.Parser.string;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.labs.parse.Parser;

import be.imgn.mtg.engine.oracle.domain.*;

/// Parsers for the [TokenDescription] body that follows "Create a …"
/// (the "5/5 green Wurm creature token with trample" shape). Produces
/// a single [TokenDescription] consumed by [EffectParsers#CREATE_TOKEN].
final class TokenDescriptionParsers {
    private TokenDescriptionParsers() {}

    /// Well-known predefined token families whose body is a single
    /// capitalized subtype followed by "token(s)". See Rule 111.10.
    private static final Parser<TokenDescription> PREDEFINED_TOKEN = Arrays.stream(PredefinedToken.values())
            .map(e -> phrase(e.text()).thenReturn(e))
            .collect(or())
            .followedBy(phrase("token(s)"))
            .map(TokenDescription::predefined);

    /// Subtype(s) + card type(s) preceding "token(s)". Returned as a pair
    /// (subtypes, cardTypes) so [#CUSTOM_TOKEN_BARE] can assemble them.
    private static final Parser<Map.Entry<List<Subtype>, List<CardType>>> TOKEN_TAIL = anyOf(
            sequence(SUBTYPE.atLeastOnce(), CARD_TYPE.atLeastOnce().followedBy(phrase("token(s)")), Map::entry),
            CARD_TYPE.atLeastOnce().followedBy(phrase("token(s)")).map(types -> Map.entry(List.<Subtype>of(), types)));

    /// Color list preceding a token body — either `colorless` (empty list)
    /// or one-or-more basic colors joined by `and` (e.g., "white and black").
    private static final Parser<List<Color>> TOKEN_COLORS =
            anyOf(phrase("colorless").thenReturn(List.<Color>of()), COLOR.atLeastOnceDelimitedBy("and"));

    /// Trailing "that's \[colors\]" tail — for tokens whose colors appear
    /// after the subtype/type/token keyword (Godsire: "Create an 8/8
    /// Beast creature token that's red, green, and white."). Accepts
    /// Oxford-comma-and-delimited colors via [MtgParsers#andList].
    private static final Parser<List<Color>> TRAILING_THATS_COLORS =
            phrase("that's").then(MtgParsers.andList(COLOR));

    /// Optional `with [keyword list]` suffix on a custom token (e.g., Advent
    /// of the Wurm: "Create a 5/5 green Wurm creature token with trample.").
    /// Emits the parsed [Ability] values directly so callers hold the
    /// enum instances (`Ability.StaticKeyword.TRAMPLE`) rather than a
    /// surface-level class-name string.
    private static final Parser<List<Ability>> TOKEN_ABILITIES = phrase("with")
            .then(KeywordParsers.KEYWORD.atLeastOnceDelimitedBy(
                    anyOf(string(","), phrase("and")), Collectors.toUnmodifiableList()));

    private static final Parser<TokenDescription.Custom> CUSTOM_TOKEN_BARE = anyOf(
            sequence(
                    PT_VALUE,
                    TOKEN_COLORS,
                    TOKEN_TAIL,
                    (pt, colors, tail) -> new TokenDescription.Custom(
                            pt, colors, List.of(), tail.getValue(), tail.getKey(), List.of())),
            // "[pt] [subtype] [card-type] token that's [colors]" —
            // trailing-colors variant (Godsire).
            sequence(
                    PT_VALUE,
                    TOKEN_TAIL,
                    TRAILING_THATS_COLORS,
                    (pt, tail, colors) -> new TokenDescription.Custom(
                            pt, colors, List.of(), tail.getValue(), tail.getKey(), List.of())));

    private static final Parser<TokenDescription> CUSTOM_TOKEN = CUSTOM_TOKEN_BARE
            .optionallyFollowedBy(TOKEN_ABILITIES, TokenDescription.Custom::withAbilities)
            .map(c -> c);

    /// "a token that's a copy of [source]" — e.g., Myr Propagator:
    /// "Create a token that's a copy of this creature.".
    private static final Parser<TokenDescription> COPY_TOKEN = phrase("token that's a copy of")
            .then(SubjectParsers.SUBJECT)
            .<TokenDescription>map(TokenDescription.CopyOf::new);

    static final Parser<TokenDescription> TOKEN_DESCRIPTION = anyOf(PREDEFINED_TOKEN, COPY_TOKEN, CUSTOM_TOKEN);
}
