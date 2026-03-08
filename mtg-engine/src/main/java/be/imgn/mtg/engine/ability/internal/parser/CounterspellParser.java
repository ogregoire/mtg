package be.imgn.mtg.engine.ability.internal.parser;

import static com.google.common.labs.parse.Parser.word;

import com.google.common.labs.parse.Parser;

import be.imgn.mtg.engine.effect.CounterSpellEffect;

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
