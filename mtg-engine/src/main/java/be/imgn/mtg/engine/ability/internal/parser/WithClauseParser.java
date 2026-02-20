package be.imgn.mtg.engine.ability.internal.parser;

import static be.imgn.mtg.parse.Parser.anyOf;
import static be.imgn.mtg.parse.Parser.sequence;
import static be.imgn.mtg.parse.Parser.word;

import java.util.List;

import be.imgn.mtg.engine.selector.WithClause;
import be.imgn.mtg.parse.Parser;

/// Parser for "with" clauses in oracle text.
public final class WithClauseParser {

    private WithClauseParser() {}

    /// Parses "mana value N or less/greater".
    public static final Parser<WithClause> MANA_VALUE = sequence(
                    word("mana"), word("value"), CommonParsers.integerWithComparison(), (a, b, cv) -> cv)
            .map(cv -> new WithClause.ManaValue(cv.comparison(), cv.value()));

    /// Parses "power N or less/greater".
    public static final Parser<WithClause> POWER = word("power")
            .then(CommonParsers.integerWithComparison())
            .map(cv -> new WithClause.Power(cv.comparison(), cv.value()));

    /// Parses "toughness N or less/greater".
    public static final Parser<WithClause> TOUGHNESS = word("toughness")
            .then(CommonParsers.integerWithComparison())
            .map(cv -> new WithClause.Toughness(cv.comparison(), cv.value()));

    /// Parses common ability names.
    public static final Parser<String> ABILITY_NAME = anyOf(
            word("flying").thenReturn("flying"),
            word("haste").thenReturn("haste"),
            word("trample").thenReturn("trample"),
            word("vigilance").thenReturn("vigilance"),
            word("lifelink").thenReturn("lifelink"),
            word("deathtouch").thenReturn("deathtouch"),
            word("menace").thenReturn("menace"),
            word("reach").thenReturn("reach"),
            word("defender").thenReturn("defender"),
            word("indestructible").thenReturn("indestructible"),
            word("hexproof").thenReturn("hexproof"));

    /// Parses ability with clause.
    public static final Parser<WithClause> ABILITY = ABILITY_NAME.map(WithClause.Ability::new);

    /// Parses counter type names.
    public static final Parser<String> COUNTER_TYPE = anyOf(
            word("+1/+1").thenReturn("+1/+1"),
            word("-1/-1").thenReturn("-1/-1"),
            word("charge").thenReturn("charge"),
            word("loyalty").thenReturn("loyalty"));

    /// Parses counter with clause.
    public static final Parser<WithClause> COUNTER = sequence(
                    anyOf(word("a"), word("an")),
                    COUNTER_TYPE,
                    word("counter"),
                    (article, counterType, counter) -> counterType)
            .map(WithClause.Counter::new);

    /// Parses a single with clause content (after "with").
    public static final Parser<WithClause> WITH_CLAUSE_CONTENT = anyOf(MANA_VALUE, POWER, TOUGHNESS, COUNTER, ABILITY);

    /// Parses "with X" clause.
    public static final Parser<WithClause> WITH_CLAUSE = word("with").then(WITH_CLAUSE_CONTENT);

    /// Parses optional list of with clauses.
    public static final Parser<List<WithClause>>.OrEmpty WITH_CLAUSES = WITH_CLAUSE.zeroOrMore();
}
