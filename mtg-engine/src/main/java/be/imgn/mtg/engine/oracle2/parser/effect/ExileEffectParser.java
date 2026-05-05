package be.imgn.mtg.engine.oracle2.parser.effect;

import static be.imgn.mtg.engine.oracle2.parser.Parsers.phrase;

import com.google.common.labs.parse.Parser;

import be.imgn.mtg.engine.oracle2.domain.effect.ExileEffect;
import be.imgn.mtg.engine.oracle2.parser.selector.SelectorParser;

/// Parser for [ExileEffect] — the verb-first imperative form
/// "Exile X." ({@mtg.rule 701.20}).
public final class ExileEffectParser {
    private ExileEffectParser() {}

    public static final Parser<ExileEffect> EXILE =
            phrase("Exile").then(SelectorParser.SELECTOR).map(ExileEffect::new);
}
