package be.imgn.mtg.engine.oracle2.parser.ability;

import static be.imgn.mtg.engine.oracle2.parser.Parsers.phrase;
import static com.google.common.labs.parse.Parser.anyOf;
import static com.google.common.labs.parse.Parser.sequence;

import com.google.common.labs.parse.Parser;

import be.imgn.mtg.engine.oracle2.domain.ability.Ability;
import be.imgn.mtg.engine.oracle2.parser.selector.SelectorParser;

/// Parser for [Ability.MaximumHandSize] — continuous max-hand-size
/// modifiers of the form "[SUBJECT] [have|has] [size] maximum hand
/// size." ({@mtg.rule 402.2}). Only the "no maximum" form lands
/// today (Graceful Adept, Spellbook); numeric and delta forms get
/// new arms when cards demand them.
public final class MaximumHandSizeParser {
    private MaximumHandSizeParser() {}

    /// Stateless size markers — "no" → [Ability.MaximumHandSize.HandSize.Standard.NONE].
    private static final Parser<Ability.MaximumHandSize.HandSize.Standard> STANDARD =
            anyOf(phrase("no").thenReturn(Ability.MaximumHandSize.HandSize.Standard.NONE));

    /// "[subject] [have|has] [size] maximum hand size." — sentence-
    /// closing period. STANDARD is used directly; its narrow type
    /// widens to [Ability.MaximumHandSize.HandSize] via the
    /// constructor reference's parameter contravariance.
    public static final Parser<Ability.MaximumHandSize> MAXIMUM_HAND_SIZE = sequence(
                    SelectorParser.SELECTOR.followedBy(phrase("[have|has]")),
                    STANDARD.followedBy(phrase("maximum hand size")),
                    Ability.MaximumHandSize::new)
            .followedBy(phrase("."));
}
