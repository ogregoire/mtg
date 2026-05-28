package be.imgn.mtg.engine.oracle2.parser.effect;

import static be.imgn.mtg.engine.oracle2.parser.AmountParser.AMOUNT;
import static be.imgn.mtg.engine.oracle2.parser.Parsers.phrase;

import java.util.function.Function;

import com.google.common.labs.parse.Parser;

import be.imgn.mtg.engine.oracle2.domain.effect.Effect;
import be.imgn.mtg.engine.oracle2.domain.effect.MillEffect;
import be.imgn.mtg.engine.oracle2.domain.selector.Selector;

/// Parser for [MillEffect] ({@mtg.rule 701.13}). Subject-led only:
/// "X mills N cards." or bare "Mill N cards." (where [EffectParser]
/// supplies the implicit "you" subject).
public final class MillEffectParser {
    private MillEffectParser() {}

    /// "mill(s) N card(s)" — subject-led wrapper. Mirrors
    /// [DrawEffectParser#DRAWS_FN].
    public static final Parser<Function<Selector, Effect>> MILLS_FN =
            EffectParser.subjectVerb(phrase("mill(s)").then(AMOUNT).followedBy(phrase("card(s)")), MillEffect::new);
}
