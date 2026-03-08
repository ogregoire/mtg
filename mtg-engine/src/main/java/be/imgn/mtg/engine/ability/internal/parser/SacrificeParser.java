package be.imgn.mtg.engine.ability.internal.parser;

import static com.google.common.labs.parse.Parser.word;

import com.google.common.labs.parse.Parser;

import be.imgn.mtg.engine.effect.SacrificeEffect;

/// Parser for sacrifice effects in oracle text.
public final class SacrificeParser {

    private SacrificeParser() {}

    /// Parses a sacrifice effect.
    ///
    /// Pattern: "Sacrifice" subject ["."]
    /// Examples:
    /// - Sacrifice a creature.
    /// - Sacrifice target permanent.
    /// - Sacrifice it.
    public static final Parser<SacrificeEffect> SACRIFICE_EFFECT = word("Sacrifice")
            .then(ReferenceParser.SUBJECT)
            .map(SacrificeEffect::new)
            .optionallyFollowedBy(".");
}
