package be.imgn.mtg.engine.ability.internal.parser;

import static com.google.common.labs.parse.Parser.anyOf;
import static com.google.common.labs.parse.Parser.string;
import static com.google.common.labs.parse.Parser.word;

import com.google.common.labs.parse.Parser;
import com.google.mu.util.CharPredicate;

import be.imgn.mtg.engine.ability.internal.parser.effect.DestroyEffect;

/// Parser for destroy effects in oracle text.
public final class DestroyParser {

    private DestroyParser() {}

    /// The skip pattern for whitespace.
    private static final CharPredicate WHITESPACE = CharPredicate.is(' ');

    /// Parses a "can't be regenerated" follow-up sentence.
    private static final Parser<Boolean> CANT_BE_REGENERATED = anyOf(
                    word("It"),
                    word("They"),
                    string("That creature"),
                    string("The creature"),
                    string("Those creatures"),
                    string("A creature destroyed this way"),
                    string("Artifacts destroyed this way"),
                    string("Creatures destroyed this way"))
            .then(string("can't be regenerated"))
            .thenReturn(false)
            .followedBy(".");

    /// Parses a destroy effect.
    ///
    /// Pattern: "Destroy" subject "." ["[subject] can't be regenerated."]
    /// Examples:
    /// - Destroy target creature.
    /// - Destroy all creatures.
    /// - Destroy it.
    /// - Destroy that creature.
    /// - Destroy all creatures. They can't be regenerated.
    public static final Parser<DestroyEffect> DESTROY_EFFECT = word("Destroy")
            .then(ReferenceParser.SUBJECT)
            .map(subject -> new DestroyEffect(subject, true))
            .followedBy(".")
            .optionallyFollowedBy(CANT_BE_REGENERATED, (effect, ignored) -> new DestroyEffect(effect.subject(), false));

    /// Parses a destroy effect from oracle text, skipping whitespace.
    ///
    /// @param oracleText the oracle text to parse
    /// @return the parsed destroy effect
    public static DestroyEffect parse(String oracleText) {
        return DESTROY_EFFECT.parseSkipping(WHITESPACE, oracleText);
    }
}
