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
    /// Single-pass: the elements are parsed once, then the connector
    /// is dispatched at its actual position (after the trailing
    /// comma, or between the two elements of a pair). The three
    /// connective spellings share the element-parsing path rather
    /// than re-parsing for each connective.
    static <T> Parser<JoinedList<T>> joinedList(Parser<T> element) {
        Parser<Mana.Connector> connector = anyOf(
                string("and/or").thenReturn(Mana.Connector.AND_OR),
                word("and").thenReturn(Mana.Connector.AND),
                word("or").thenReturn(Mana.Connector.OR));
        // 3+ elements: "A, B, ..., <connector> Z"
        var threeOrMore = sequence(element.followedBy(",").atLeastOnce(), connector, element, (heads, conn, tail) -> {
            var jl = new JoinedList<T>().connector(conn);
            heads.forEach(jl::add);
            return jl.add(tail);
        });
        // 2 elements: "A <connector> B"
        var pair = sequence(element, connector, element, (a, conn, b) -> new JoinedList<T>()
                .connector(conn)
                .add(a)
                .add(b));
        // 1 element: connector remains null.
        var single = element.map(t -> new JoinedList<T>().add(t));
        return anyOf(threeOrMore, pair, single);
    }

    private static <T> List<T> append(List<T> heads, T tail) {
        var list = new ArrayList<>(heads);
        list.add(tail);
        return List.copyOf(list);
    }
}
