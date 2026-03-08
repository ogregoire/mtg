package be.imgn.mtg.engine.ability.internal.parser;

import static com.google.common.labs.parse.Parser.word;

import com.google.common.labs.parse.Parser;

import be.imgn.mtg.engine.effect.TapEffect;
import be.imgn.mtg.engine.effect.UntapEffect;

/// Parser for tap and untap effects in oracle text.
public final class TapParser {

    private TapParser() {}

    /// Parses a tap effect.
    ///
    /// Pattern: "Tap" subject ["."]
    /// Examples:
    /// - Tap target creature.
    /// - Tap all creatures.
    /// - Tap it.
    public static final Parser<TapEffect> TAP_EFFECT =
            word("Tap").then(ReferenceParser.SUBJECT).map(TapEffect::new).optionallyFollowedBy(".");

    /// Parses an untap effect.
    ///
    /// Pattern: "Untap" subject ["."]
    /// Examples:
    /// - Untap target creature.
    /// - Untap all lands you control.
    /// - Untap it.
    public static final Parser<UntapEffect> UNTAP_EFFECT =
            word("Untap").then(ReferenceParser.SUBJECT).map(UntapEffect::new).optionallyFollowedBy(".");
}
