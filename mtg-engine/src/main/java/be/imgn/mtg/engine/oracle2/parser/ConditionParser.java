package be.imgn.mtg.engine.oracle2.parser;

import static be.imgn.mtg.engine.oracle2.parser.AmountMatcherParser.AMOUNT_MATCHER;
import static be.imgn.mtg.engine.oracle2.parser.Parsers.phrase;
import static com.google.common.labs.parse.Parser.anyOf;
import static com.google.common.labs.parse.Parser.sequence;

import com.google.common.labs.parse.Parser;

import be.imgn.mtg.engine.oracle2.domain.Condition;
import be.imgn.mtg.engine.oracle2.parser.selector.SelectorParser;

/// Parser for [Condition] — the predicate body of an intervening-if
/// clause ({@mtg.rule 603.4}). The leading `if` and trailing comma
/// are owned by
/// [be.imgn.mtg.engine.oracle2.parser.ability.AbilityParser]; this
/// parser handles only the predicate text between them.
///
/// One arm today: [#HAS_LIFE] for `<selector> [has|have] <matcher>
/// life` (Felidar Sovereign, Test of Endurance). Future condition
/// shapes get added as new private parsers and folded into
/// [#CONDITION]'s `anyOf`.
public final class ConditionParser {
    private ConditionParser() {}

    /// `<selector> [has|have] <matcher> life` — [Condition.HasLife].
    private static final Parser<Condition.HasLife> HAS_LIFE = sequence(
            SelectorParser.SELECTOR,
            phrase("[has|have]").then(AMOUNT_MATCHER).followedBy(phrase("life")),
            Condition.HasLife::new);

    /// Top-level [Condition]. Currently dispatches to a single arm;
    /// shape future-proofed for additional condition records.
    public static final Parser<Condition> CONDITION = anyOf(HAS_LIFE);
}
