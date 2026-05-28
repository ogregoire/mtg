package be.imgn.mtg.engine.oracle2.parser.effect;

import static be.imgn.mtg.engine.oracle2.parser.Parsers.phrase;

import java.util.function.Function;

import com.google.common.labs.parse.Parser;

import be.imgn.mtg.engine.oracle2.domain.effect.Effect;
import be.imgn.mtg.engine.oracle2.domain.effect.PlayWithHandsRevealedEffect;
import be.imgn.mtg.engine.oracle2.domain.selector.Selector;

/// Parser for [PlayWithHandsRevealedEffect] ({@mtg.rule 408}).
/// Subject-led: "(who) play(s) with their hands revealed."
/// Revelation: "Players play with their hands revealed." — the
/// "their" anaphoric possessive is silently consumed (the parent
/// `who` is the hand-owner).
public final class PlayWithHandsRevealedEffectParser {
    private PlayWithHandsRevealedEffectParser() {}

    /// "play(s) with their hands revealed" — subject-led wrapper.
    public static final Parser<Function<Selector, Effect>> PLAY_HANDS_REVEALED_FN = EffectParser.subjectVerbNoArg(
            phrase("play(s) with their hands revealed"), PlayWithHandsRevealedEffect::new);
}
