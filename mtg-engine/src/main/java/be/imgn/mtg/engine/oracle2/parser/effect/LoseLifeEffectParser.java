package be.imgn.mtg.engine.oracle2.parser.effect;

import static be.imgn.mtg.engine.oracle2.parser.AmountParser.AMOUNT;
import static be.imgn.mtg.engine.oracle2.parser.Parsers.phrase;

import java.util.function.Function;

import com.google.common.labs.parse.Parser;

import be.imgn.mtg.engine.oracle2.domain.effect.Effect;
import be.imgn.mtg.engine.oracle2.domain.effect.LoseLifeEffect;
import be.imgn.mtg.engine.oracle2.domain.selector.Selector;

/// Parser for [LoseLifeEffect] ({@mtg.rule 119.3}). Always
/// subject-led ("Target player loses N life.").
public final class LoseLifeEffectParser {
    private LoseLifeEffectParser() {}

    /// "lose(s) N life" — subject-led wrapper.
    public static final Parser<Function<Selector, Effect>> LOSES_LIFE_FN =
            EffectParser.subjectVerb(phrase("lose(s)").then(AMOUNT).followedBy(phrase("life")), LoseLifeEffect::new);
}
