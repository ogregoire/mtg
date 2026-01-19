package be.imgn.mtg.engine.ability.internal.parser;

import static be.imgn.mtg.parse.Parser.sequence;

import java.util.Optional;

import be.imgn.mtg.engine.ability.internal.parser.selector.ObjectSelector;
import be.imgn.mtg.parse.Both;
import be.imgn.mtg.parse.Parser;

/// Parser for object selectors in oracle text.
public final class ObjectSelectorParser {

    private ObjectSelectorParser() {}

    /// Parses a complete object selector.
    ///
    /// Pattern: `[quantifier] [qualifiers] type [with clauses] [controller clause]`
    ///
    /// Examples:
    /// - target creature
    /// - all creatures
    /// - target nonland permanent with mana value 3 or less
    /// - each creature that player controls
    public static final Parser<ObjectSelector> OBJECT_SELECTOR = sequence(
            sequence(QuantifierParser.QUANTIFIER, QualifierParser.QUALIFIER.zeroOrMore(), Both::of)
                    .notEmpty(),
            sequence(
                    sequence(TypeParser.TYPE_MATCHER, WithClauseParser.WITH_CLAUSES, Both::of),
                    ControllerParser.CONTROLLER_CLAUSE,
                    Both::of),
            (qAndQ, tAndWAndC) -> {
                var quantifier = qAndQ.first();
                var qualifiers = qAndQ.second();
                var typeMatcher = tAndWAndC.first().first();
                var withClauses = tAndWAndC.first().second();
                var controller = tAndWAndC.second();
                return new ObjectSelector(
                        quantifier, qualifiers, typeMatcher, withClauses, Optional.ofNullable(controller));
            });
}
