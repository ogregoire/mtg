package be.imgn.mtg.engine.ability.internal.parser;

import static com.google.common.labs.parse.Parser.sequence;

import com.google.common.labs.parse.Parser;
import com.google.mu.util.Both;

import be.imgn.mtg.engine.selector.ObjectSelector;

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
                    sequence(ObjectTypeParser.TYPE_MATCHER, WithClauseParser.WITH_CLAUSES, Both::of),
                    ControllerParser.CONTROLLER_CLAUSE,
                    Both::of),
            (qAndQ, tAndWAndC) -> qAndQ.andThen((quantifier, qualifiers) ->
                    tAndWAndC.andThen((typeAndWith, controller) -> typeAndWith.andThen((typeMatcher, withClauses) ->
                            new ObjectSelector(quantifier, qualifiers, typeMatcher, withClauses, controller)))));
}
