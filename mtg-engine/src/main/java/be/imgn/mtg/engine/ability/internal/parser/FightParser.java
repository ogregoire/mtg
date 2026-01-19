package be.imgn.mtg.engine.ability.internal.parser;

import static be.imgn.mtg.parse.Parser.sequence;
import static be.imgn.mtg.parse.Parser.word;

import be.imgn.mtg.engine.ability.internal.parser.effect.FightEffect;
import be.imgn.mtg.parse.Parser;

/// Parser for fight effects in oracle text.
public final class FightParser {

    private FightParser() {}

    /// Parses "Target creature you control fights target creature."
    ///
    /// Pattern: subject "fights" subject ["."]
    public static final Parser<FightEffect> FIGHT_EFFECT = sequence(
                    ReferenceParser.SUBJECT, word("fights").then(ReferenceParser.SUBJECT), FightEffect::new)
            .optionallyFollowedBy(".");
}
