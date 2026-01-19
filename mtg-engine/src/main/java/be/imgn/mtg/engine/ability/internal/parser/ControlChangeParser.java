package be.imgn.mtg.engine.ability.internal.parser;

import static be.imgn.mtg.parse.Parser.sequence;
import static be.imgn.mtg.parse.Parser.string;

import be.imgn.mtg.engine.ability.internal.parser.effect.GainControlEffect;
import be.imgn.mtg.parse.Parser;

/// Parser for control change effects in oracle text.
public final class ControlChangeParser {

    private ControlChangeParser() {}

    /// Parses "Gain control of target creature."
    ///
    /// Pattern: "Gain control of" subject ["until end of turn"] ["."]
    public static final Parser<GainControlEffect> GAIN_CONTROL_EFFECT = string("Gain control of")
            .then(sequence(ReferenceParser.SUBJECT, DurationParser.DURATION.optional(), GainControlEffect::new))
            .optionallyFollowedBy(".");
}
