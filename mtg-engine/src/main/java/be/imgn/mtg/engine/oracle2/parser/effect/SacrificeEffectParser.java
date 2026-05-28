package be.imgn.mtg.engine.oracle2.parser.effect;

import static be.imgn.mtg.engine.oracle2.parser.Parsers.phrase;

import java.util.function.Function;

import com.google.common.labs.parse.Parser;

import be.imgn.mtg.engine.oracle2.domain.effect.Effect;
import be.imgn.mtg.engine.oracle2.domain.effect.SacrificeEffect;
import be.imgn.mtg.engine.oracle2.domain.selector.Selector;
import be.imgn.mtg.engine.oracle2.parser.selector.SelectorParser;

/// Parser for [SacrificeEffect] ({@mtg.rule 701.21}). Always
/// subject-led ("X sacrifices Y."); no verb-first imperative form.
public final class SacrificeEffectParser {
    private SacrificeEffectParser() {}

    /// "sacrifice(s) SELECTOR" — subject-led wrapper.
    public static final Parser<Function<Selector, Effect>> SACRIFICES_FN =
            EffectParser.subjectVerb(phrase("sacrifice(s)").then(SelectorParser.SELECTOR), SacrificeEffect::new);
}
