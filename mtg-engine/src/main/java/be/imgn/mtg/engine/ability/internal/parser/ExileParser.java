package be.imgn.mtg.engine.ability.internal.parser;

import static com.google.common.labs.parse.Parser.word;

import com.google.common.labs.parse.Parser;

import be.imgn.mtg.engine.ability.internal.parser.effect.ExileEffect;

/// Parser for exile effects in oracle text.
public final class ExileParser {

    private ExileParser() {}

    /// Parses an exile effect.
    ///
    /// Pattern: "Exile" subject ["."]
    /// Examples:
    /// - Exile target creature.
    /// - Exile all creatures.
    /// - Exile it.
    /// - Exile target nonblack creature.
    public static final Parser<ExileEffect> EXILE_EFFECT =
            word("Exile").then(ReferenceParser.SUBJECT).map(ExileEffect::new).optionallyFollowedBy(".");
}
