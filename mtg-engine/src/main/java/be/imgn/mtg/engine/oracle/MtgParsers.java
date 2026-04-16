package be.imgn.mtg.engine.oracle;

import static com.google.common.labs.parse.Parser.anyOf;
import static com.google.common.labs.parse.Parser.sequence;
import static com.google.common.labs.parse.Parser.string;
import static com.google.common.labs.parse.Parser.word;

import java.util.ArrayList;
import java.util.List;

import com.google.common.labs.parse.Parser;

/// Combinators for common oracle text patterns.
final class MtgParsers {
    private MtgParsers() {}

    /// Parses an Oxford-comma list of {@code element} joined by {@code connector}.
    ///
    /// Matches:
    /// - 1 element: {@code A}
    /// - 2 elements: {@code A connector B}
    /// - 3+ elements: {@code A, B, ..., connector Z} (Oxford comma)
    private static <T> Parser<List<T>> list(Parser<T> element, Parser<?> connector) {
        var threeOrMore = sequence(
                element.followedBy(",").atLeastOnce(),
                connector.then(element),
                (List<T> heads, T tail) -> append(heads, tail));
        var pair = sequence(element, connector.then(element), List::of);
        var single = element.map(List::of);
        return anyOf(threeOrMore, pair, single);
    }

    /// Oxford-comma list with "and": {@code A}, {@code A and B}, {@code A, B, and C}.
    static <T> Parser<List<T>> andList(Parser<T> element) {
        return list(element, word("and"));
    }

    /// Oxford-comma list with "or": {@code A}, {@code A or B}, {@code A, B, or C}.
    static <T> Parser<List<T>> orList(Parser<T> element) {
        return list(element, word("or"));
    }

    /// List with "and/or": {@code A} or {@code A and/or B}.
    static <T> Parser<List<T>> andOrList(Parser<T> element) {
        return list(element, string("and/or"));
    }

    private static <T> List<T> append(List<T> heads, T tail) {
        var list = new ArrayList<>(heads);
        list.add(tail);
        return List.copyOf(list);
    }
}
