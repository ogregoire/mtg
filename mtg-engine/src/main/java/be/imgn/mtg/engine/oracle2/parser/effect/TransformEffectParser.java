package be.imgn.mtg.engine.oracle2.parser.effect;

import static be.imgn.mtg.engine.oracle2.parser.Parsers.phrase;
import static com.google.common.labs.parse.Parser.sequence;

import java.util.function.Function;

import com.google.common.labs.parse.Parser;

import be.imgn.mtg.engine.oracle2.domain.effect.TransformEffect;
import be.imgn.mtg.engine.oracle2.domain.selector.Selector;
import be.imgn.mtg.engine.oracle2.parser.selector.SelectorParser;

/// Parser for [TransformEffect] — the verb-first imperative form
/// "Transform X." ({@mtg.rule 701.28}). Mirrors [DestroyEffectParser]
/// shape so the generic verb-choice parser in [EffectParser] can
/// reuse [#TRANSFORM_VERB] for "transform or X target ..."
/// composition if any oracle text ever needs it.
public final class TransformEffectParser {
    private TransformEffectParser() {}

    /// Verb-only constructor — `phrase("Transform")` produces a
    /// builder that takes a target selector and returns a
    /// [TransformEffect].
    static final Parser<Function<Selector, TransformEffect>> TRANSFORM_VERB =
            phrase("Transform").thenReturn(TransformEffect::new);

    /// "Transform SELECTOR" — full sentence; period is consumed at
    /// the [EffectParser] level.
    public static final Parser<TransformEffect> TRANSFORM =
            sequence(TRANSFORM_VERB, SelectorParser.SELECTOR, Function::apply);
}
