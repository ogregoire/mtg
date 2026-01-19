package be.imgn.mtg.engine.ability.internal.parser;

import static be.imgn.mtg.parse.Parser.word;

import be.imgn.mtg.engine.ability.internal.parser.effect.CounterSpellEffect;
import be.imgn.mtg.parse.Parser;

/// Parser for counterspell effects in oracle text.
public final class CounterspellParser {

    private CounterspellParser() {}

    /// Parses "Counter target spell."
    ///
    /// Pattern: "Counter" subject ["."]
    public static final Parser<CounterSpellEffect> COUNTER_SPELL_EFFECT = word("Counter")
            .then(ReferenceParser.SUBJECT)
            .map(CounterSpellEffect::new)
            .optionallyFollowedBy(".");
}
