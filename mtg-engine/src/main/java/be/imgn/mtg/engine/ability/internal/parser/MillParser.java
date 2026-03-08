package be.imgn.mtg.engine.ability.internal.parser;

import static com.google.common.labs.parse.Parser.anyOf;
import static com.google.common.labs.parse.Parser.sequence;
import static com.google.common.labs.parse.Parser.string;
import static com.google.common.labs.parse.Parser.word;

import java.util.Optional;

import com.google.common.labs.parse.Parser;

import be.imgn.mtg.engine.effect.MillEffect;
import be.imgn.mtg.engine.selector.PlayerReference;

/// Parser for mill effects in oracle text.
public final class MillParser {

    private MillParser() {}

    /// Parses a player reference for mill.
    private static final Parser<Optional<PlayerReference>> MILL_PLAYER = anyOf(
                    string("Target player").thenReturn(PlayerReference.TARGET_PLAYER),
                    string("Each player").thenReturn(PlayerReference.EACH_PLAYER),
                    string("that player").thenReturn(PlayerReference.THAT_PLAYER))
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
