package be.imgn.mtg.engine.oracle;

import static com.google.common.labs.parse.Parser.anyOf;
import static com.google.common.labs.parse.Parser.or;
import static com.google.common.labs.parse.Parser.word;

import java.util.Arrays;
import java.util.function.BiFunction;

import com.google.common.labs.parse.Parser;
import com.google.mu.util.CharPredicate;
import com.google.mu.util.Substring;

/// Case-insensitive word matching utilities for oracle text parsing.
final class Words {
    private Words() {}

    private static final Substring.RepeatingPattern WHITESPACE =
            Substring.consecutive(Character::isWhitespace).repeatedly();

    /// Match a word case-insensitively (both title-case and lowercase).
    static Parser<String> w(String text) {
        var lower = text.toLowerCase();
        var title = Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
        if (lower.equals(title)) return word(lower);
        return anyOf(word(title), word(lower));
    }

    /// Parse a sequence of case-sensitive words written as a single string.
    /// Returns the input string on match.
    /// {@code words("the battlefield")} is equivalent to
    /// {@code word("the").then(word("battlefield")).thenReturn("the battlefield")}.
    static Parser<String> words(String s) {
        return WHITESPACE
                .split(s)
                .map(m -> word(m.toString()))
                .reduce(Parser::then)
                .orElseThrow()
                .thenReturn(s);
    }

    /// Parse a sequence of case-insensitive words written as a single string.
    /// Returns the input string on match.
    /// {@code ciWords("any target")} matches "Any Target", "any target", etc.
    static Parser<String> ciWords(String s) {
        return WHITESPACE
                .split(s)
                .map(m -> Parser.caseInsensitiveWord(m.toString()))
                .reduce(Parser::then)
                .orElseThrow()
                .thenReturn(s);
    }

    /// Match any of the given case-sensitive words. Returns the matched word.
    static Parser<String> anyWord(String... alternatives) {
        return Arrays.stream(alternatives).map(Parser::word).collect(or());
    }

    /// Match any of the given words case-insensitively. Returns the matched word (lowercase).
    static Parser<String> anyCiWord(String... alternatives) {
        return Arrays.stream(alternatives).map(Words::w).collect(or());
    }

    /// Match any of the given case-sensitive word sequences. Returns the matched sequence.
    static Parser<String> anySentence(String... alternatives) {
        return Arrays.stream(alternatives).map(Words::words).collect(or());
    }

    /// Match any of the given word sequences case-insensitively. Returns the matched sequence.
    static Parser<String> anyCiSentence(String... alternatives) {
        return Arrays.stream(alternatives).map(Words::ciWords).collect(or());
    }

    /// {@code sequence} with an optional left and a required right.
    /// Mirrors the package-private {@code Parser.sequence(OrEmpty, Parser, BiFunction)}.
    static <A, B, C> Parser<C> sequence(
            Parser<A>.OrEmpty left, Parser<B> right, BiFunction<? super A, ? super B, ? extends C> combiner) {
        var defaultLeft = left.parseSkipping(CharPredicate.is(' '), "");
        return anyOf(Parser.sequence(left.notEmpty(), right, combiner), right.map(b -> combiner.apply(defaultLeft, b)));
    }
}
