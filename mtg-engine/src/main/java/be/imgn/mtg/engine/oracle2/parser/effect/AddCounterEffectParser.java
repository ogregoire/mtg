package be.imgn.mtg.engine.oracle2.parser.effect;

import static be.imgn.mtg.engine.oracle2.parser.AmountParser.AMOUNT;
import static be.imgn.mtg.engine.oracle2.parser.Parsers.phrase;
import static com.google.common.labs.parse.Parser.sequence;

import com.google.common.labs.parse.Parser;

import be.imgn.mtg.engine.oracle2.domain.effect.AddCounterEffect;
import be.imgn.mtg.engine.oracle2.parser.selector.ObjectCounterSelectorParser;
import be.imgn.mtg.engine.oracle2.parser.selector.SelectorParser;

/// Parser for [AddCounterEffect] ({@mtg.rule 122.2}). Verb-first
/// shape "Put (count) (type) counter(s) on (target)." Reuses
/// [ObjectCounterSelectorParser#COUNTER_TYPE] for the type slot so
/// every CounterType arm (+1/+1, -1/-1, loyalty, charge, …) is
/// recognised without duplication.
public final class AddCounterEffectParser {
    private AddCounterEffectParser() {}

    /// "Put (count) (type) counter(s) on (target)" — full sentence;
    /// period is consumed at the [EffectParser] level.
    public static final Parser<AddCounterEffect> ADD_COUNTER = sequence(
            phrase("Put").then(AMOUNT),
            ObjectCounterSelectorParser.COUNTER_TYPE.followedBy(phrase("counter(s) on")),
            SelectorParser.SELECTOR,
            AddCounterEffect::new);
}
