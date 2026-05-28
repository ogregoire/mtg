package be.imgn.mtg.engine.oracle2.parser.effect;

import static be.imgn.mtg.engine.oracle2.parser.Parsers.phrase;
import static com.google.common.labs.parse.Parser.sequence;

import java.util.function.Function;

import com.google.common.labs.parse.Parser;

import be.imgn.mtg.engine.oracle2.domain.effect.FlipEffect;
import be.imgn.mtg.engine.oracle2.domain.selector.Selector;
import be.imgn.mtg.engine.oracle2.parser.selector.SelectorParser;

/// Parser for [FlipEffect] ({@mtg.rule 110.5b}). Verb-first
/// imperative form "Flip X." Student of Elements: "When this
/// creature has flying, flip it." — the trigger embeds the flip
/// effect with `it` as the target.
public final class FlipEffectParser {
    private FlipEffectParser() {}

    /// Verb-only constructor.
    static final Parser<Function<Selector, FlipEffect>> FLIP_VERB =
            phrase("Flip").thenReturn(FlipEffect::new);

    /// "Flip SELECTOR" — full sentence; period consumed at the
    /// [EffectParser] level.
    public static final Parser<FlipEffect> FLIP = sequence(FLIP_VERB, SelectorParser.SELECTOR, Function::apply);
}
