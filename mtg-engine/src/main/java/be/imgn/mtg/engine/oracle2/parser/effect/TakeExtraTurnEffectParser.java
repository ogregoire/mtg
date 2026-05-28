package be.imgn.mtg.engine.oracle2.parser.effect;

import static be.imgn.mtg.engine.oracle2.parser.Parsers.phrase;

import java.util.function.Function;

import com.google.common.labs.parse.Parser;

import be.imgn.mtg.engine.oracle2.domain.effect.Effect;
import be.imgn.mtg.engine.oracle2.domain.effect.TakeExtraTurnEffect;
import be.imgn.mtg.engine.oracle2.domain.selector.Selector;

/// Parser for [TakeExtraTurnEffect] ({@mtg.rule 500.7}). Subject-led
/// only: "(who) take(s) an extra turn after this one." or the bare
/// "Take an extra turn after this one." (where [EffectParser] supplies
/// the implicit "you" subject). No per-clause data — the no-arg
/// [EffectParser#subjectVerb] overload handles the bind.
public final class TakeExtraTurnEffectParser {
    private TakeExtraTurnEffectParser() {}

    /// "take(s) an extra turn after this one" — subject-led wrapper.
    public static final Parser<Function<Selector, Effect>> TAKE_EXTRA_TURN_FN =
            EffectParser.subjectVerbNoArg(phrase("take(s) an extra turn after this one"), TakeExtraTurnEffect::new);
}
