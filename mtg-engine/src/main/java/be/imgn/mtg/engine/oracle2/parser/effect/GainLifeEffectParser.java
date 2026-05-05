package be.imgn.mtg.engine.oracle2.parser.effect;

import static be.imgn.mtg.engine.oracle2.parser.AmountParser.AMOUNT;
import static be.imgn.mtg.engine.oracle2.parser.Parsers.phrase;

import java.util.function.Function;

import com.google.common.labs.parse.Parser;

import be.imgn.mtg.engine.oracle2.domain.effect.Effect;
import be.imgn.mtg.engine.oracle2.domain.effect.GainLifeEffect;
import be.imgn.mtg.engine.oracle2.domain.selector.Selector;

/// Parser for [GainLifeEffect] ({@mtg.rule 119.3}). Always
/// subject-led ("You gain N life.").
public final class GainLifeEffectParser {
    private GainLifeEffectParser() {}

    /// "gain(s) N life" — subject-led wrapper.
    public static final Parser<Function<Selector, Effect>> GAINS_LIFE_FN =
            EffectParser.subjectVerb(phrase("gain(s)").then(AMOUNT).followedBy(phrase("life")), GainLifeEffect::new);
}
