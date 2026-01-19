package be.imgn.mtg.engine.ability.internal.parser;

import static be.imgn.mtg.parse.Parser.word;

import be.imgn.mtg.engine.ability.internal.parser.effect.DestroyEffect;
import be.imgn.mtg.parse.CharPredicate;
import be.imgn.mtg.parse.Parser;

/// Parser for destroy effects in oracle text.
public final class DestroyParser {

    private DestroyParser() {}

    /// The skip pattern for whitespace.
    private static final CharPredicate WHITESPACE = CharPredicate.is(' ');

    /// Parses a destroy effect.
    ///
    /// Pattern: "Destroy" subject ["."]
    /// Examples:
    /// - Destroy target creature.
    /// - Destroy all creatures.
    /// - Destroy it.
    /// - Destroy that creature.
    public static final Parser<DestroyEffect> DESTROY_EFFECT = word("Destroy")
            .then(ReferenceParser.SUBJECT)
            .map(DestroyEffect::new)
            .optionallyFollowedBy(".");

    /// Parses a destroy effect from oracle text, skipping whitespace.
    ///
    /// @param oracleText the oracle text to parse
    /// @return the parsed destroy effect
    public static DestroyEffect parse(String oracleText) {
        return DESTROY_EFFECT.parseSkipping(WHITESPACE, oracleText);
    }
}
