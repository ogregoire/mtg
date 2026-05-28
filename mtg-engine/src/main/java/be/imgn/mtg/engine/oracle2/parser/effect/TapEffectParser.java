package be.imgn.mtg.engine.oracle2.parser.effect;

import static be.imgn.mtg.engine.oracle2.parser.Parsers.phrase;
import static com.google.common.labs.parse.Parser.sequence;

import java.util.function.Function;

import com.google.common.labs.parse.Parser;

import be.imgn.mtg.engine.oracle2.domain.effect.TapEffect;
import be.imgn.mtg.engine.oracle2.domain.selector.Selector;
import be.imgn.mtg.engine.oracle2.parser.selector.SelectorParser;

/// Parser for [TapEffect] — the verb-first imperative "Tap X."
/// ({@mtg.rule 701.26}).
///
/// The verb constructor [#TAP_VERB] is factored out so the generic
/// `\<Verb\> or \<Verb\> \<target\>` choice in [EffectParser] can
/// reuse the same literal "Tap" → [TapEffect] mapping without
/// duplicating the phrase template.
public final class TapEffectParser {
    private TapEffectParser() {}

    /// Verb-only constructor — `phrase("Tap")` → builder that
    /// produces a [TapEffect] given a target selector. Used by
    /// [#TAP] and by the multi-verb dispatch in [EffectParser].
    static final Parser<Function<Selector, TapEffect>> TAP_VERB = phrase("Tap").thenReturn(TapEffect::new);

    /// "Tap SELECTOR" — full sentence is closed by the trailing
    /// period at the [EffectParser] level.
    public static final Parser<TapEffect> TAP = sequence(TAP_VERB, SelectorParser.SELECTOR, Function::apply);
}
