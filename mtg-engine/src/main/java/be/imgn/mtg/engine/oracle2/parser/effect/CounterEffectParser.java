package be.imgn.mtg.engine.oracle2.parser.effect;

import static be.imgn.mtg.engine.oracle2.parser.Parsers.phrase;
import static com.google.common.labs.parse.Parser.sequence;

import java.util.function.Function;

import com.google.common.labs.parse.Parser;

import be.imgn.mtg.engine.oracle2.domain.Condition;
import be.imgn.mtg.engine.oracle2.domain.effect.CounterEffect;
import be.imgn.mtg.engine.oracle2.domain.selector.Selector;
import be.imgn.mtg.engine.oracle2.parser.selector.SelectorParser;

/// Parser for [CounterEffect] — the verb-first imperative form
/// "Counter X (if (condition))?." ({@mtg.rule 701.5}). The optional
/// "if (condition)" tail (Ertai's Trickery: "Counter target spell
/// if it was kicked.") attaches a [Condition] that the engine
/// re-checks at resolution time.
///
/// The verb constructor [#COUNTER_VERB] is factored out so the
/// generic verb-choice parser in [EffectParser] can reuse it for
/// "counter or X target …" composition.
public final class CounterEffectParser {
    private CounterEffectParser() {}

    /// Verb-only constructor — `phrase("Counter")` → builder that
    /// produces a [CounterEffect] given a target selector.
    static final Parser<Function<Selector, CounterEffect>> COUNTER_VERB =
            phrase("Counter").thenReturn(CounterEffect::new);

    /// "if (subject) was kicked" — currently the only [Condition]
    /// the counter parser recognises. New arms can join this anyOf
    /// as more "Counter ... if ..." cards land.
    private static final Parser<Condition> COUNTER_IF_CONDITION = phrase("if")
            .then(SelectorParser.SELECTOR)
            .followedBy(phrase("was kicked"))
            .<Condition>map(Condition.WasKicked::new);

    /// "Counter SELECTOR (if (condition))?" — full sentence is
    /// closed by the trailing period at the [EffectParser] level.
    public static final Parser<CounterEffect> COUNTER = sequence(COUNTER_VERB, SelectorParser.SELECTOR, Function::apply)
            .optionallyFollowedBy(COUNTER_IF_CONDITION, CounterEffect::withCondition);
}
