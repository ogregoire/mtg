package be.imgn.mtg.engine.oracle2.parser.effect;

import static be.imgn.mtg.engine.oracle2.parser.Parsers.phrase;
import static com.google.common.labs.parse.Parser.sequence;

import java.util.function.Function;

import com.google.common.labs.parse.Parser;

import be.imgn.mtg.engine.oracle2.domain.effect.DestroyEffect;
import be.imgn.mtg.engine.oracle2.domain.selector.Selector;
import be.imgn.mtg.engine.oracle2.parser.selector.SelectorParser;

/// Parser for [DestroyEffect] — the verb-first imperative form
/// "Destroy X." ({@mtg.rule 701.8}).
///
/// The verb constructor [#DESTROY_VERB] is factored out so the
/// generic verb-choice parser in [EffectParser] can reuse it for
/// "destroy or X target …" composition.
public final class DestroyEffectParser {
    private DestroyEffectParser() {}

    /// Verb-only constructor — `phrase("Destroy")` → builder that
    /// produces a [DestroyEffect] given a target selector.
    static final Parser<Function<Selector, DestroyEffect>> DESTROY_VERB =
            phrase("Destroy").thenReturn(DestroyEffect::new);

    /// "Destroy SELECTOR" — full sentence is closed by the trailing
    /// period at the [EffectParser] level.
    public static final Parser<DestroyEffect> DESTROY =
            sequence(DESTROY_VERB, SelectorParser.SELECTOR, Function::apply);
}
