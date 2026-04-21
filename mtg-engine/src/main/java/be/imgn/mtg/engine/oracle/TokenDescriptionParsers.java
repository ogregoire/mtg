package be.imgn.mtg.engine.oracle;

import static be.imgn.mtg.engine.oracle.Words.ciWords;
import static be.imgn.mtg.engine.oracle.Words.phrase;
import static be.imgn.mtg.engine.oracle.Words.w;
import static com.google.common.labs.parse.Parser.anyOf;
import static com.google.common.labs.parse.Parser.sequence;
import static com.google.common.labs.parse.Parser.string;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.labs.parse.Parser;

/// Parsers for the [TokenDescription] body that follows "Create a …"
/// (the "5/5 green Wurm creature token with trample" shape). Produces
/// a single [TokenDescription] consumed by [EffectParsers#CREATE_TOKEN].
final class TokenDescriptionParsers {
    private TokenDescriptionParsers() {}

    /// Well-known predefined token families whose body is a single
    /// capitalized subtype followed by "token(s)".
    private static final Parser<TokenDescription> PREDEFINED_TOKEN = anyOf(
                    w("Treasure"),
                    w("Food"),
                    w("Gold"),
                    w("Clue"),
                    w("Blood"),
                    w("Powerstone"),
                    w("Map"),
                    w("Incubator"))
            .followedBy(phrase("token(s)"))
            .map(TokenDescription::predefined);

    /// Subtype(s) + card type(s) preceding "token(s)". Returned as a pair
    /// (subtypes, cardTypes) so [#CUSTOM_TOKEN_BARE] can assemble them.
    private static final Parser<Map.Entry<List<Subtype>, List<CardType>>> TOKEN_TAIL = anyOf(
            sequence(
                    SelectorParsers.SUBTYPE.atLeastOnce(),
                    SelectorParsers.CARD_TYPE.atLeastOnce().followedBy(phrase("token(s)")),
                    Map::entry),
            SelectorParsers.CARD_TYPE
                    .atLeastOnce()
                    .followedBy(phrase("token(s)"))
                    .map(types -> Map.entry(List.<Subtype>of(), types)));

    /// Color list preceding a token body — either `colorless` (empty list)
    /// or one-or-more basic colors joined by `and` (e.g., "white and black").
    private static final Parser<List<Color>> TOKEN_COLORS =
            anyOf(w("colorless").thenReturn(List.<Color>of()), SelectorParsers.COLOR.atLeastOnceDelimitedBy("and"));

    /// Optional `with [keyword list]` suffix on a custom token (e.g., Advent
    /// of the Wurm: "Create a 5/5 green Wurm creature token with trample.").
    /// Emits the list of ability names so consumers can reconstruct the
    /// token's printed text — the grammar doesn't yet try to resolve each
    /// keyword to its structured [Ability] form in this context.
    private static final Parser<List<String>> TOKEN_ABILITIES = w("with")
            .then(KeywordParsers.KEYWORD.atLeastOnceDelimitedBy(
                    anyOf(string(","), w("and")), Collectors.toUnmodifiableList()))
            .map(list -> list.stream().map(a -> a.getClass().getSimpleName()).toList());

    private static final Parser<TokenDescription.Custom> CUSTOM_TOKEN_BARE = sequence(
            SelectorParsers.PT_VALUE,
            TOKEN_COLORS,
            TOKEN_TAIL,
            (pt, colors, tail) ->
                    new TokenDescription.Custom(pt, colors, List.of(), tail.getValue(), tail.getKey(), List.of()));

    private static final Parser<TokenDescription> CUSTOM_TOKEN = CUSTOM_TOKEN_BARE
            .optionallyFollowedBy(TOKEN_ABILITIES, TokenDescription.Custom::withAbilities)
            .map(c -> c);

    /// "a token that's a copy of [source]" — e.g., Myr Propagator:
    /// "Create a token that's a copy of this creature.".
    private static final Parser<TokenDescription> COPY_TOKEN = ciWords("token that's a copy of")
            .then(SubjectParsers.SUBJECT)
            .<TokenDescription>map(TokenDescription.CopyOf::new);

    static final Parser<TokenDescription> TOKEN_DESCRIPTION = anyOf(PREDEFINED_TOKEN, COPY_TOKEN, CUSTOM_TOKEN);
}
