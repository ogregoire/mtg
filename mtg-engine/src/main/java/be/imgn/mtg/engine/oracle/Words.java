package be.imgn.mtg.engine.oracle;

import static com.google.common.labs.parse.Parser.anyOf;
import static com.google.common.labs.parse.Parser.consecutive;
import static com.google.common.labs.parse.Parser.or;
import static com.google.common.labs.parse.Parser.string;
import static com.google.common.labs.parse.Parser.word;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;

import com.google.common.labs.parse.CharacterSet;
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

    // ── phrase() — template-driven oracle-text phrase parser ──────────

    /// Parse an oracle-text phrase written in a small template DSL.
    /// Returns a parser that emits the {@code template} string unchanged
    /// on a successful match.
    ///
    /// Syntax:
    /// - `Word` (first token only, leading uppercase) → case-insensitive
    ///   match for {@code word} at sentence start.
    /// - `word` (anywhere else, or first token lowercase) → case-sensitive
    ///   match for the exact word.
    /// - `word(suffix)` → matches {@code wordsuffix} or {@code word}
    ///   (`anyWord("wordsuffix", "word")`). Common uses: `(s)` for
    ///   regular plurals, `(es)` for plurals of words ending in -s/-sh/
    ///   -ch/-x, `(ing)` / `(ed)` for verb inflections.
    /// - `[a|b]` → alternatives (required); matches {@code a} or {@code b}.
    /// - `[a b c]` → required multi-word phrase.
    /// - `word?`, `[...]?` → optional suffix on the preceding token.
    ///
    /// The first token cannot be optional — there's no
    /// `optionallyPrecededBy`. If you need the whole phrase to be
    /// optional at a call site, use {@code optionallyFollowedBy} on the
    /// parser that precedes it.
    ///
    /// Examples:
    /// ```
    /// phrase("Destroy creature(s) you control")
    /// phrase("deal(s) [combat]? damage to")
    /// phrase("Target creature(s) [is|are] blocked")
    /// ```
    static Parser<String> phrase(String template) {
        List<PhraseToken> tokens;
        try {
            tokens = TEMPLATE.parseSkipping(CharPredicate.is(' '), template);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("invalid phrase: \"" + template + "\": " + e.getMessage(), e);
        }
        if (tokens.get(0).optional()) {
            throw new IllegalArgumentException(
                    "phrase cannot start with an optional token; use optionallyFollowedBy on the preceding parser"
                            + " instead: \"" + template + "\"");
        }
        Parser<?> head = buildParser(tokens.get(0), startsWithUppercase(tokens.get(0)));
        for (int i = 1; i < tokens.size(); i++) {
            var t = tokens.get(i);
            var p = buildParser(t, false);
            head = t.optional() ? head.optionallyFollowedBy(p, (a, _) -> a) : head.then(p);
        }
        return head.thenReturn(template);
    }

    private static boolean startsWithUppercase(PhraseToken t) {
        return switch (t) {
            case PlainToken(var text, var _) -> Character.isUpperCase(text.charAt(0));
            case BracketToken(var text, var _) -> Character.isUpperCase(text.charAt(0));
        };
    }

    private static Parser<?> buildParser(PhraseToken t, boolean ci) {
        return switch (t) {
            case PlainToken(var text, var _) -> buildPlain(text, ci);
            case BracketToken(var text, var _) -> buildBracket(text, ci);
        };
    }

    private static Parser<String> buildPlain(String text, boolean ci) {
        var open = text.indexOf('(');
        if (open >= 0 && text.endsWith(")")) {
            var base = text.substring(0, open);
            var suffix = text.substring(open + 1, text.length() - 1);
            return ci ? anyCiWord(base + suffix, base) : anyWord(base + suffix, base);
        }
        return ci ? w(text) : word(text);
    }

    private static Parser<String> buildBracket(String text, boolean ci) {
        if (text.contains("|")) {
            var alts = text.split("\\|");
            for (var alt : alts) {
                if (alt.contains(" ")) return ci ? anyCiSentence(alts) : anySentence(alts);
            }
            return ci ? anyCiWord(alts) : anyWord(alts);
        }
        return ci ? ciWords(text) : words(text);
    }

    // ── Template grammar (parsed at phrase() construction time) ───────

    private sealed interface PhraseToken {
        boolean optional();
    }

    private record PlainToken(String text, boolean optional) implements PhraseToken {}

    private record BracketToken(String text, boolean optional) implements PhraseToken {}

    /// One template word — letters, digits, apostrophes, or dashes, with
    /// an optional {@code (suffix)} inflection marker. Common forms are
    /// {@code (s)} for regular plurals and {@code (es)} / {@code (ing)}
    /// / {@code (ed)} for other inflections.
    private static final Parser<String> WORD_INFLECTION =
            consecutive(CharacterSet.charsIn("[A-Za-z]"), "inflection").immediatelyBetween("(", ")");

    private static final Parser<String> TEMPLATE_WORD = consecutive(
                    CharacterSet.charsIn("[A-Za-z0-9'-]"), "phrase word")
            .optionallyFollowedBy(WORD_INFLECTION, (w, inflection) -> w + "(" + inflection + ")");

    /// Plain token: a word, optionally flagged optional by a trailing
    /// {@code ?}.
    private static final Parser<PhraseToken> PLAIN_TOKEN_RULE = TEMPLATE_WORD
            .<PhraseToken>map(w -> new PlainToken(w, false))
            .optionallyFollowedBy(string("?"), (t, _) -> new PlainToken(((PlainToken) t).text(), true));

    /// Bracket token: {@code [...]} with optional trailing {@code ?}.
    /// The content is captured verbatim (pipes and internal spaces
    /// preserved) and interpreted by {@link #buildBracket}.
    private static final Parser<PhraseToken> BRACKET_TOKEN_RULE = consecutive(
                    CharPredicate.noneOf("]"), "bracket content")
            .immediatelyBetween("[", "]")
            .<PhraseToken>map(text -> new BracketToken(text.trim(), false))
            .optionallyFollowedBy(string("?"), (t, _) -> new BracketToken(((BracketToken) t).text(), true));

    private static final Parser<List<PhraseToken>> TEMPLATE =
            anyOf(BRACKET_TOKEN_RULE, PLAIN_TOKEN_RULE).atLeastOnce();
}
