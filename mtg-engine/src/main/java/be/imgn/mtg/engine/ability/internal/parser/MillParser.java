package be.imgn.mtg.engine.ability.internal.parser;

import static be.imgn.mtg.parse.Parser.anyOf;
import static be.imgn.mtg.parse.Parser.sequence;
import static be.imgn.mtg.parse.Parser.word;

import java.util.Optional;

import be.imgn.mtg.engine.ability.internal.parser.effect.MillEffect;
import be.imgn.mtg.engine.ability.internal.parser.reference.PlayerReference;
import be.imgn.mtg.parse.Parser;

/// Parser for mill effects in oracle text.
public final class MillParser {

    private MillParser() {}

    /// Parses a player reference for mill.
    private static final Parser<Optional<PlayerReference>> MILL_PLAYER = anyOf(
                    word("Target player").thenReturn(PlayerReference.TARGET_PLAYER),
                    word("Each player").thenReturn(PlayerReference.EACH_PLAYER),
                    word("that player").thenReturn(PlayerReference.THAT_PLAYER))
            .map(Optional::of);

    /// Parses "cards" suffix.
    private static final Parser<String> CARDS_SUFFIX = anyOf(word("cards"), word("card"));

    /// Parses a mill effect.
    ///
    /// Pattern: `[Player] "mill" / "mills" amount ["cards"] ["."]`
    ///
    /// Examples:
    /// - Mill three cards.
    /// - Target player mills five cards.
    /// - Mill X cards.
    public static final Parser<MillEffect> MILL_EFFECT = anyOf(
                    // "Target player mills five cards"
                    sequence(
                            MILL_PLAYER,
                            word("mills").then(AmountParser.AMOUNT).followedBy(CARDS_SUFFIX.orElse("")),
                            MillEffect::new),
                    // "Mill three cards" (implicit you)
                    word("Mill")
                            .then(AmountParser.AMOUNT)
                            .followedBy(CARDS_SUFFIX.orElse(""))
                            .map(amount -> new MillEffect(Optional.empty(), amount)))
            .optionallyFollowedBy(".");
}
