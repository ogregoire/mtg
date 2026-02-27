package be.imgn.mtg.engine.ability.internal.parser;

import static com.google.common.labs.parse.Parser.anyOf;
import static com.google.common.labs.parse.Parser.digits;
import static com.google.common.labs.parse.Parser.sequence;
import static com.google.common.labs.parse.Parser.single;
import static com.google.common.labs.parse.Parser.string;
import static com.google.common.labs.parse.Parser.word;

import com.google.common.labs.parse.Parser;
import com.google.mu.util.CharPredicate;

import be.imgn.mtg.engine.ability.internal.parser.effect.AddCountersEffect;
import be.imgn.mtg.engine.ability.internal.parser.effect.RemoveCountersEffect;
import be.imgn.mtg.engine.ability.internal.parser.selector.Amount;
import be.imgn.mtg.engine.characteristics.CounterType;

/// Parser for counter effects in oracle text.
public final class CounterParser {

    private CounterParser() {}

    /// Parses a signed number like "+1" or "-1".
    private static final Parser<String> SIGNED_NUMBER = sequence(
            single(CharPredicate.is('+').or('-'), "+/-").map(String::valueOf), digits(), (sign, num) -> sign + num);

    /// Parses a P/T counter like "+1/+1" or "-1/-1".
    private static final Parser<String> PT_COUNTER =
            sequence(SIGNED_NUMBER, string("/").then(SIGNED_NUMBER), (first, second) -> first + "/" + second);

    /// Parses a counter type like "+1/+1", "-1/-1", "loyalty", "charge".
    /// The parser returns a string which is then converted to a CounterType.
    private static final Parser<String> COUNTER_TYPE_TEXT = anyOf(
            PT_COUNTER,
            word("loyalty").thenReturn("loyalty"),
            word("charge").thenReturn("charge"),
            word("poison").thenReturn("poison"),
            word("age").thenReturn("age"),
            word("time").thenReturn("time"),
            word("quest").thenReturn("quest"),
            word("level").thenReturn("level"),
            word("lore").thenReturn("lore"),
            word("energy").thenReturn("energy"),
            word("experience").thenReturn("experience"),
            word("shield").thenReturn("shield"),
            word("stun").thenReturn("stun"),
            word("defense").thenReturn("defense"),
            word("finality").thenReturn("finality"),
            word("rad").thenReturn("rad"),
            word("ticket").thenReturn("ticket"),
            word("fade").thenReturn("fade"),
            word("storage").thenReturn("storage"),
            word("spore").thenReturn("spore"),
            word("verse").thenReturn("verse"),
            word("ki").thenReturn("ki"),
            word("blood").thenReturn("blood"),
            word("bounty").thenReturn("bounty"),
            word("luck").thenReturn("luck"));

    /// Parses a counter type and converts to CounterType.
    private static final Parser<CounterType> COUNTER_TYPE = COUNTER_TYPE_TEXT.map(CounterType::of);

    /// Parses "counter" or "counters".
    private static final Parser<String> COUNTER_WORD = anyOf(word("counters"), word("counter"));

    /// Parses an amount for counters ("a" = 1, or numeric).
    private static final Parser<Amount> COUNTER_AMOUNT =
            anyOf(word("a").thenReturn(new Amount.Exact(1)), AmountParser.AMOUNT);

    /// Parses "Put a +1/+1 counter on target creature."
    ///
    /// Pattern: "Put" amount type "counter(s) on" subject ["."]
    public static final Parser<AddCountersEffect> ADD_COUNTERS_EFFECT = word("Put")
            .then(sequence(
                    COUNTER_AMOUNT,
                    COUNTER_TYPE.followedBy(COUNTER_WORD).followedBy(word("on")),
                    ReferenceParser.SUBJECT,
                    AddCountersEffect::new))
            .optionallyFollowedBy(".");

    /// Parses "Remove a +1/+1 counter from target creature."
    ///
    /// Pattern: "Remove" amount type "counter(s) from" subject ["."]
    public static final Parser<RemoveCountersEffect> REMOVE_COUNTERS_EFFECT = word("Remove")
            .then(sequence(
                    COUNTER_AMOUNT,
                    COUNTER_TYPE.followedBy(COUNTER_WORD).followedBy(string("from")),
                    ReferenceParser.SUBJECT,
                    RemoveCountersEffect::new))
            .optionallyFollowedBy(".");
}
