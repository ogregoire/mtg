package be.imgn.mtg.engine.ability.internal.parser;

import static be.imgn.mtg.parse.Parser.anyOf;
import static be.imgn.mtg.parse.Parser.sequence;
import static be.imgn.mtg.parse.Parser.string;
import static be.imgn.mtg.parse.Parser.word;

import java.util.Optional;

import be.imgn.mtg.engine.ability.internal.parser.effect.DiscardEffect;
import be.imgn.mtg.engine.ability.internal.parser.effect.DrawEffect;
import be.imgn.mtg.engine.ability.internal.parser.effect.ScryEffect;
import be.imgn.mtg.engine.ability.internal.parser.effect.SearchLibraryEffect;
import be.imgn.mtg.engine.ability.internal.parser.selector.Amount;
import be.imgn.mtg.engine.selector.PlayerReference;
import be.imgn.mtg.parse.Parser;

/// Parser for draw, discard, and scry effects in oracle text.
public final class DrawParser {

    private DrawParser() {}

    /// Parses a player reference.
    private static final Parser<PlayerReference> PLAYER_REF = anyOf(
            string("Target player").thenReturn(PlayerReference.TARGET_PLAYER),
            string("target player").thenReturn(PlayerReference.TARGET_PLAYER),
            string("Each player").thenReturn(PlayerReference.EACH_PLAYER),
            string("each player").thenReturn(PlayerReference.EACH_PLAYER),
            string("that player").thenReturn(PlayerReference.THAT_PLAYER));

    /// Parses "cards" or "card" suffix.
    private static final Parser<String> CARDS_SUFFIX = anyOf(word("cards"), word("card"));

    /// Parses a draw with "a card" as amount 1.
    private static final Parser<Amount> DRAW_AMOUNT = anyOf(
            string("a card").thenReturn(new Amount.Exact(1)), AmountParser.AMOUNT.followedBy(CARDS_SUFFIX.orElse("")));

    /// Parses "Draw two cards." or "Target player draws a card."
    ///
    /// Pattern: `[Player] "draw"/"draws" amount ["cards"] ["."]`
    public static final Parser<DrawEffect> DRAW_EFFECT = anyOf(
                    // "Target player draws three cards"
                    sequence(
                            PLAYER_REF,
                            word("draws").then(DRAW_AMOUNT),
                            (player, amount) -> new DrawEffect(Optional.of(player), amount)),
                    // "Draw two cards" (implicit you)
                    word("Draw").then(DRAW_AMOUNT).map(amount -> new DrawEffect(Optional.empty(), amount)))
            .optionallyFollowedBy(".");

    /// Parses a discard amount.
    private static final Parser<Amount> DISCARD_AMOUNT = anyOf(
            string("a card").thenReturn(new Amount.Exact(1)), AmountParser.AMOUNT.followedBy(CARDS_SUFFIX.orElse("")));

    /// Parses "Discard a card." or "Target player discards two cards."
    ///
    /// Pattern: `[Player] "discard"/"discards" amount ["cards"] ["."]`
    public static final Parser<DiscardEffect> DISCARD_EFFECT = anyOf(
                    // "Target player discards two cards"
                    sequence(
                            PLAYER_REF,
                            word("discards").then(DISCARD_AMOUNT),
                            (player, amount) -> new DiscardEffect(Optional.of(player), amount)),
                    // "Discard a card" (implicit you)
                    word("Discard").then(DISCARD_AMOUNT).map(amount -> new DiscardEffect(Optional.empty(), amount)))
            .optionallyFollowedBy(".");

    /// Parses "Scry 2." or "Scry X."
    ///
    /// Pattern: "Scry" amount ["."]
    public static final Parser<ScryEffect> SCRY_EFFECT =
            word("Scry").then(AmountParser.AMOUNT).map(ScryEffect::new).optionallyFollowedBy(".");

    /// Parses "Search your library for a card" or "Search your library for a creature card".
    ///
    /// Pattern: `"Search your library for" ["a"/"an"] [type] "card" ["."]`
    public static final Parser<SearchLibraryEffect> SEARCH_LIBRARY_EFFECT = string("Search your library for")
            .then(anyOf(
                    // "a card" with no type
                    string("a card").thenReturn(new SearchLibraryEffect(Optional.empty())),
                    // "a creature card", "an artifact card", etc.
                    anyOf(word("a"), word("an"))
                            .then(TypeParser.TYPE_OR_TYPE)
                            .followedBy(word("card"))
                            .map(type -> new SearchLibraryEffect(Optional.of(type)))))
            .optionallyFollowedBy(".");
}
