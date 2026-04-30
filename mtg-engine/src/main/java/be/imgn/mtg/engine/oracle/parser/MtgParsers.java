package be.imgn.mtg.engine.oracle.parser;

import static com.google.common.labs.parse.Parser.anyOf;
import static com.google.common.labs.parse.Parser.one;
import static com.google.common.labs.parse.Parser.sequence;
import static com.google.common.labs.parse.Parser.string;
import static com.google.common.labs.parse.Parser.word;

import java.util.ArrayList;
import java.util.List;

import com.google.common.labs.parse.Parser;

import be.imgn.mtg.engine.oracle.domain.Mana;

/// Combinators for common oracle text patterns.
final class MtgParsers {
    private MtgParsers() {}

    /// Parses an Oxford-comma list of `element` joined by `connector`.
    ///
    /// Matches:
    /// - 1 element: `A`
    /// - 2 elements: `A connector B`
    /// - 3+ elements: `A, B, ..., connector Z` (Oxford comma)
    private static <T> Parser<List<T>> list(Parser<T> element, Parser<?> connector) {
        var threeOrMore = sequence(
                element.followedBy(",").atLeastOnce(),
                connector.then(element),
                (List<T> heads, T tail) -> append(heads, tail));
        var pair = sequence(element, connector.then(element), List::of);
        var single = element.map(List::of);
        return anyOf(threeOrMore, pair, single);
    }

    /// Oxford-comma list with "and": `A`, `A and B`, `A, B, and C`.
    static <T> Parser<List<T>> andList(Parser<T> element) {
        return list(element, word("and"));
    }

    /// Oxford-comma list with "and" or "then" as the final connector —
    /// `A, B, and C` or `A, B, then C` (Decimator Web: "loses 2 life,
    /// gets a poison counter, then mills six cards."). "then" is the
    /// sequential-emphasis variant of "and" and is interchangeable at
    /// the chain level.
    static <T> Parser<List<T>> andOrThenList(Parser<T> element) {
        return list(element, anyOf(word("and"), word("then")));
    }

    /// Oxford-comma list with "or": `A`, `A or B`, `A, B, or C`.
    static <T> Parser<List<T>> orList(Parser<T> element) {
        return list(element, word("or"));
    }

    /// List with "and/or": `A` or `A and/or B`.
    static <T> Parser<List<T>> andOrList(Parser<T> element) {
        return list(element, string("and/or"));
    }

    /// Single comma — list-element separator. Hoisted so each
    /// `joinedList` instantiation reuses the same parser instance.
    private static final Parser<Character> COMMA = one(',');

    /// English connective for [#joinedList]: matches "and/or", "and",
    /// or "or" and returns the corresponding [Mana.Connector] enum.
    /// Hoisted to a static field so the inner `anyOf` is built once
    /// for the lifetime of the parser tree, not on each `joinedList`
    /// instantiation.
    private static final Parser<Mana.Connector> CONNECTOR = anyOf(
            string("and/or").thenReturn(Mana.Connector.AND_OR),
            word("and").thenReturn(Mana.Connector.AND),
            word("or").thenReturn(Mana.Connector.OR));

    /// Oxford-comma list joined by any of the connective spellings
    /// "and/or", "and", or "or". Returns the items paired with the
    /// [Mana.Connector] kind that matched, so the caller can preserve
    /// which spelling the oracle used (or branch on it). Used for the
    /// mana-combination clause ("in any combination of {R} and/or
    /// {G}", theoretically "{R} and {G}" or "{R} or {G}").
    ///
    /// Single-pass parse of the first element: the first element is
    /// parsed exactly once, then `optionallyFollowedBy` dispatches on
    /// what follows — a comma (three-or-more form), a connector (pair
    /// form), or nothing (single-element form). Each tail arm builds
    /// its own [JoinedList] for the elements after the first; the
    /// combiner merges items + copies the connector.
    static <T> Parser<JoinedList<T>> joinedList(Parser<T> element) {
        Parser<JoinedList<T>> threeOrMoreTail = sequence(
                COMMA.then(element).atLeastOnce(),
                COMMA.then(CONNECTOR),
                element,
                (middle, conn, last) ->
                        new JoinedList<T>().addAll(middle).connector(conn).add(last));
        Parser<JoinedList<T>> pairTail = sequence(CONNECTOR, element, (conn, last) -> new JoinedList<T>()
                .connector(conn)
                .add(last));
        var tail = anyOf(threeOrMoreTail, pairTail);
        return element.<JoinedList<T>>map(first -> new JoinedList<T>().add(first))
                .optionallyFollowedBy(tail, JoinedList::merge);
    }

    private static <T> List<T> append(List<T> heads, T tail) {
        var list = new ArrayList<>(heads);
        list.add(tail);
        return List.copyOf(list);
    }
}
