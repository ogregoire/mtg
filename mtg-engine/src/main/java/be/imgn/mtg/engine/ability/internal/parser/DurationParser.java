package be.imgn.mtg.engine.ability.internal.parser;

import static be.imgn.mtg.parse.Parser.anyOf;
import static be.imgn.mtg.parse.Parser.string;

import be.imgn.mtg.engine.ability.internal.parser.selector.Duration;
import be.imgn.mtg.parse.Parser;

/// Parser for effect durations in oracle text.
public final class DurationParser {

    private DurationParser() {}

    /// Parses "until end of turn".
    public static final Parser<Duration> UNTIL_END_OF_TURN =
            string("until end of turn").thenReturn(new Duration.UntilEndOfTurn());

    /// Parses "until your next turn".
    public static final Parser<Duration> UNTIL_YOUR_NEXT_TURN =
            string("until your next turn").thenReturn(new Duration.UntilYourNextTurn());

    /// Parses a duration phrase.
    public static final Parser<Duration> DURATION = anyOf(UNTIL_END_OF_TURN, UNTIL_YOUR_NEXT_TURN);
}
