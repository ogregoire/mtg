package be.imgn.mtg.engine.oracle2.parser.effect;

import static be.imgn.mtg.engine.oracle2.parser.Parsers.phrase;
import static com.google.common.labs.parse.Parser.sequence;

import java.util.function.Function;

import com.google.common.labs.parse.Parser;

import be.imgn.mtg.engine.oracle2.domain.effect.UntapEffect;
import be.imgn.mtg.engine.oracle2.domain.selector.Selector;
import be.imgn.mtg.engine.oracle2.parser.selector.SelectorParser;

/// Parser for [UntapEffect] — the verb-first imperative "Untap X."
/// ({@mtg.rule 701.26}).
///
/// The verb constructor [#UNTAP_VERB] is factored out so the generic
/// `\<Verb\> or \<Verb\> \<target\>` choice in [EffectParser] can
/// reuse the same literal "Untap" → [UntapEffect] mapping without
/// duplicating the phrase template.
public final class UntapEffectParser {
    private UntapEffectParser() {}

    /// Verb-only constructor — `phrase("Untap")` → builder that
    /// produces an [UntapEffect] given a target selector. Used by
    /// [#UNTAP] and by the multi-verb dispatch in [EffectParser].
    static final Parser<Function<Selector, UntapEffect>> UNTAP_VERB =
            phrase("Untap").thenReturn(UntapEffect::new);

    /// "Untap SELECTOR" — full sentence is closed by the trailing
    /// period at the [EffectParser] level.
    public static final Parser<UntapEffect> UNTAP = sequence(UNTAP_VERB, SelectorParser.SELECTOR, Function::apply);
}
