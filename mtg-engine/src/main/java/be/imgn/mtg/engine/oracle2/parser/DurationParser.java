package be.imgn.mtg.engine.oracle2.parser;

import static be.imgn.mtg.engine.oracle2.parser.Parsers.phrase;
import static com.google.common.labs.parse.Parser.anyOf;

import com.google.common.labs.parse.Parser;

import be.imgn.mtg.engine.oracle2.domain.Duration;

/// Parser for [Duration]. Today the grammar covers the
/// parameterless temporal scopes in [Duration.Fixed] (minus
/// [Duration.Fixed#PERMANENT], which has no oracle phrase — it's
/// the absent case). New arms land here as cards demand them.
public final class DurationParser {
    private DurationParser() {}

    /// One [Duration] phrase. PERMANENT is intentionally absent —
    /// callers should attach the duration as `.orElse(PERMANENT)`
    /// (or via `optionallyFollowedBy`) so the default is built into
    /// the call site, not duplicated as an arm here.
    public static final Parser<Duration> DURATION = anyOf(phrase("this turn").thenReturn(Duration.Fixed.THIS_TURN));
}
