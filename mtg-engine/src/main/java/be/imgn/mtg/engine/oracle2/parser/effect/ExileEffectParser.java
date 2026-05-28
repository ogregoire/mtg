package be.imgn.mtg.engine.oracle2.parser.effect;

import static be.imgn.mtg.engine.oracle2.parser.Parsers.phrase;
import static com.google.common.labs.parse.Parser.anyOf;
import static com.google.common.labs.parse.Parser.sequence;

import java.util.function.Function;

import com.google.common.labs.parse.Parser;

import be.imgn.mtg.engine.oracle2.domain.StandardQuantifier;
import be.imgn.mtg.engine.oracle2.domain.effect.ExileEffect;
import be.imgn.mtg.engine.oracle2.domain.selector.ObjectPropertySelector;
import be.imgn.mtg.engine.oracle2.domain.selector.ObjectTypeSelector;
import be.imgn.mtg.engine.oracle2.domain.selector.PlayerSelector;
import be.imgn.mtg.engine.oracle2.domain.selector.QuantifierSelector;
import be.imgn.mtg.engine.oracle2.domain.selector.Selector;
import be.imgn.mtg.engine.oracle2.domain.selector.ZoneSelector;
import be.imgn.mtg.engine.oracle2.parser.selector.SelectorParser;

/// Parser for [ExileEffect] — the verb-first imperative form
/// "Exile X." ({@mtg.rule 701.13}).
///
/// The verb constructor [#EXILE_VERB] is factored out so the
/// generic verb-choice parser in [EffectParser] can reuse it for
/// "exile or X target …" composition.
public final class ExileEffectParser {
    private ExileEffectParser() {}

    /// Verb-only constructor — `phrase("Exile")` → builder that
    /// produces an [ExileEffect] given a target selector.
    static final Parser<Function<Selector, ExileEffect>> EXILE_VERB =
            phrase("Exile").thenReturn(ExileEffect::new);

    /// "all graveyards" — every card in every graveyard, modeled as
    /// [StandardQuantifier#ALL] over [ZoneSelector.Graveyard] owned
    /// by [PlayerSelector.Anyone#ANYONE]. Used by "Exile all
    /// graveyards." (Morningtide). Bare possessive-zone phrase with
    /// no leading object-type noun, so the general SELECTOR grammar
    /// doesn't reach it.
    private static final Parser<Selector> ALL_GRAVEYARDS = phrase("all graveyards")
            .thenReturn(new QuantifierSelector(
                    StandardQuantifier.ALL,
                    new ZoneSelector.Graveyard(
                            PlayerSelector.Anyone.ANYONE,
                            new ObjectTypeSelector.Card(ObjectPropertySelector.Anything.ANYTHING))));

    /// "Exile SELECTOR" — full sentence is closed by the trailing
    /// period at the [EffectParser] level.
    public static final Parser<ExileEffect> EXILE =
            sequence(EXILE_VERB, anyOf(ALL_GRAVEYARDS, SelectorParser.SELECTOR), Function::apply);
}
