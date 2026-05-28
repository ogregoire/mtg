package be.imgn.mtg.engine.oracle2.parser.selector;

import static be.imgn.mtg.engine.oracle2.parser.AmountMatcherParser.AMOUNT_MATCHER;
import static be.imgn.mtg.engine.oracle2.parser.NumberParser.SIGNED_INT;
import static be.imgn.mtg.engine.oracle2.parser.Parsers.phrase;
import static com.google.common.labs.parse.Parser.anyOf;
import static com.google.common.labs.parse.Parser.or;
import static com.google.common.labs.parse.Parser.sequence;
import static com.google.common.labs.parse.Parser.string;

import java.util.stream.Stream;

import com.google.common.labs.parse.Parser;

import be.imgn.mtg.engine.oracle2.domain.Amount;
import be.imgn.mtg.engine.oracle2.domain.AmountMatcher;
import be.imgn.mtg.engine.oracle2.domain.CounterType;
import be.imgn.mtg.engine.oracle2.domain.Parseable;
import be.imgn.mtg.engine.oracle2.domain.selector.ObjectCounterSelector;

/// Parser for [ObjectCounterSelector]. One arm with two oracle
/// shapes:
///
/// - **Presence shorthand**: `with a/an [type] counter` →
///   `HasCounters(type, AtLeast(Exact(1)))`. The article "a/an"
///   semantically means presence (at least one).
/// - **General**: `with [matcher] [type]? counter(s)` →
///   `HasCounters(type|Any.ANY, matcher)`. The type slot is optional
///   when the matcher is "no" or count-only ("with no counters").
///
/// The trailing "on it" / "on \[selector\]" suffix is intentionally
/// not consumed — it's implicit (the counters are on the object
/// being selected). Cards that put counters on a *different* object
/// are out of scope for this arm.
public final class ObjectCounterSelectorParser {
    private ObjectCounterSelectorParser() {}

    /// `[+-]N/[+-]M` — power/toughness counter pattern (e.g. `+1/+1`,
    /// `-1/-1`). Built from [be.imgn.mtg.engine.oracle2.parser.NumberParser#SIGNED_INT].
    private static final Parser<CounterType> PT_COUNTER = sequence(
            SIGNED_INT.followedBy(string("/")), SIGNED_INT, (p, t) -> (CounterType) new CounterType.PtCounter(p, t));

    /// All named counter types (Keyword + Named enums) — one
    /// `phrase()` per value. Title-or-lower handles both
    /// "Loyalty counters" sentence-start and "loyalty counters"
    /// mid-sentence.
    static final Parser<CounterType> NAMED_OR_KEYWORD = Stream.<Parseable>concat(
                    Stream.of(CounterType.Keyword.values()), Stream.of(CounterType.Named.values()))
            .map(c -> phrase(c.text()).<CounterType>thenReturn((CounterType) c))
            .collect(or());

    /// Top-level [CounterType] for the type slot in counter
    /// selectors. PtCounter first (numeric prefix is unambiguous),
    /// then named/keyword. `Any.ANY` is NOT in this dispatch — the
    /// general arm handles missing-type via fallback.
    public static final Parser<CounterType> COUNTER_TYPE = anyOf(PT_COUNTER, NAMED_OR_KEYWORD);

    /// "with a/an [type] counter" — presence form. Maps to
    /// `AtLeast(1)`.
    private static final Parser<ObjectCounterSelector.HasCounters> PRESENCE = phrase("with [a|an]")
            .then(COUNTER_TYPE)
            .followedBy(phrase("counter"))
            .map(t -> new ObjectCounterSelector.HasCounters(t, new AmountMatcher.AtLeast(new Amount.Exact(1))));

    /// "with [matcher]" — leading prefix shared by typed and
    /// untyped general arms.
    private static final Parser<AmountMatcher> WITH_MATCHER = phrase("with").then(AMOUNT_MATCHER);

    /// `[type]? counter(s)` — type optional, defaulting to
    /// [CounterType.Any#ANY]. Built with
    /// [Parser#sequence(Parser.OrEmpty, Parser, BiFunction)] (mug 10.0).
    static final Parser<CounterType> OPT_TYPED_COUNTER =
            sequence(COUNTER_TYPE.orElse(CounterType.Any.ANY), phrase("counter(s)"), (type, _) -> type);

    /// "with [matcher] [type]? counter(s)" — general form. Type
    /// missing → [CounterType.Any#ANY].
    private static final Parser<ObjectCounterSelector.HasCounters> GENERAL = sequence(
            WITH_MATCHER, OPT_TYPED_COUNTER, (matcher, type) -> new ObjectCounterSelector.HasCounters(type, matcher));

    /// Top-level [ObjectCounterSelector]. PRESENCE first (longer
    /// specific "with a/an" prefix), then GENERAL.
    public static final Parser<ObjectCounterSelector> OBJECT_COUNTER_SELECTOR = anyOf(PRESENCE, GENERAL);
}
