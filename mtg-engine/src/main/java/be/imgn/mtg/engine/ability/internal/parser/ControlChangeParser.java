package be.imgn.mtg.engine.ability.internal.parser;

import static com.google.common.labs.parse.Parser.sequence;
import static com.google.common.labs.parse.Parser.string;

import com.google.common.labs.parse.Parser;

import be.imgn.mtg.engine.effect.GainControlEffect;

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
