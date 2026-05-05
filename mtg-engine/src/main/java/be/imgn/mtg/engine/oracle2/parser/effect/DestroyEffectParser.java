package be.imgn.mtg.engine.oracle2.parser.effect;

import static be.imgn.mtg.engine.oracle2.parser.Parsers.phrase;

import com.google.common.labs.parse.Parser;

import be.imgn.mtg.engine.oracle2.domain.effect.DestroyEffect;
import be.imgn.mtg.engine.oracle2.parser.selector.SelectorParser;

/// Parser for [DestroyEffect] — the verb-first imperative form
/// "Destroy X." ({@mtg.rule 701.7}).
public final class DestroyEffectParser {
    private DestroyEffectParser() {}

    /// "Destroy SELECTOR" — full sentence is closed by the trailing
    /// period at the [EffectParser] level.
    public static final Parser<DestroyEffect> DESTROY =
            phrase("Destroy").then(SelectorParser.SELECTOR).map(DestroyEffect::new);
}
