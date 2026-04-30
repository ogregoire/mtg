package be.imgn.mtg.engine.oracle.parser;

import static com.google.common.labs.parse.Parser.anyOf;
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
    /// form), or nothing (single-element form). Dispatch decisions
    /// happen at the connector position rather than retrying the
    /// whole element-parsing branch per connector spelling.
    static <T> Parser<JoinedList<T>> joinedList(Parser<T> element) {
        Parser<Mana.Connector> connector = anyOf(
                string("and/or").thenReturn(Mana.Connector.AND_OR),
                word("and").thenReturn(Mana.Connector.AND),
                word("or").thenReturn(Mana.Connector.OR));
        // After the first element, the optional tail is either:
        //   (a) "(", "element)+ "," connector element"  — Oxford-comma chain (3+ elements)
        //   (b) "connector element"                        — pair
        // Without a tail, the result is a single-element list.
        var threeOrMoreTail = sequence(
                string(",").then(element).atLeastOnce(),
                string(",").then(connector),
                element,
                JoinedListTail::new);
        var pairTail = sequence(connector, element, (conn, last) -> new JoinedListTail<T>(List.of(), conn, last));
        var tail = Parser.<JoinedListTail<T>>anyOf(threeOrMoreTail, pairTail);
        return element.<JoinedList<T>>map(first -> new JoinedList<T>().add(first))
                .optionallyFollowedBy(tail, (jl, t) -> jl.addAll(t.middle())
                        .connector(t.connector())
                        .add(t.last()));
    }

    /// Internal plumbing for [#joinedList] — captures the optional
    /// tail after the first element so the first element doesn't need
    /// to be re-parsed across alternative connector spellings.
    private record JoinedListTail<T>(List<T> middle, Mana.Connector connector, T last) {}

    private static <T> List<T> append(List<T> heads, T tail) {
        var list = new ArrayList<>(heads);
        list.add(tail);
        return List.copyOf(list);
    }
}
