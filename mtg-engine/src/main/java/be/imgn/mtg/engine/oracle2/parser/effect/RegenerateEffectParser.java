package be.imgn.mtg.engine.oracle2.parser.effect;

import static be.imgn.mtg.engine.oracle2.parser.Parsers.phrase;
import static com.google.common.labs.parse.Parser.sequence;

import java.util.function.Function;

import com.google.common.labs.parse.Parser;

import be.imgn.mtg.engine.oracle2.domain.effect.RegenerateEffect;
import be.imgn.mtg.engine.oracle2.domain.selector.Selector;
import be.imgn.mtg.engine.oracle2.parser.selector.SelectorParser;

/// Parser for [RegenerateEffect] — the verb-first imperative form
/// "Regenerate X." ({@mtg.rule 701.15}).
///
/// The verb constructor [#REGENERATE_VERB] is factored out so the
/// generic verb-choice parser in [EffectParser] can reuse it for
/// "regenerate or X target …" composition.
public final class RegenerateEffectParser {
    private RegenerateEffectParser() {}

    /// Verb-only constructor — `phrase("Regenerate")` → builder that
    /// produces a [RegenerateEffect] given a target selector.
    static final Parser<Function<Selector, RegenerateEffect>> REGENERATE_VERB =
            phrase("Regenerate").thenReturn(RegenerateEffect::new);

    /// "Regenerate SELECTOR" — full sentence is closed by the trailing
    /// period at the [EffectParser] level.
    public static final Parser<RegenerateEffect> REGENERATE =
            sequence(REGENERATE_VERB, SelectorParser.SELECTOR, Function::apply);
}
