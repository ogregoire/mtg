package be.imgn.mtg.engine.oracle2.parser;

import static com.google.common.labs.parse.Parser.anyOf;
import static com.google.common.labs.parse.Parser.consecutive;
import static com.google.common.labs.parse.Parser.or;
import static com.google.common.labs.parse.Parser.sequence;
import static com.google.common.labs.parse.Parser.string;
import static com.google.common.labs.parse.Parser.word;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.common.labs.parse.CharacterSet;
import com.google.common.labs.parse.Parser;
import com.google.mu.util.CharPredicate;

/// Shared dot-parse utilities for the oracle2 parser tree. Independent
/// of `oracle.parser` and `oracle.domain` — copied here so the oracle2
/// tree stays self-contained.
///
/// Two groups of utilities:
/// - [#phrase(String)] — template-driven oracle-text phrase parser
///   (inflection, alternation, optional tokens).
/// - [#andList], [#orList], [#andOrList] — Oxford-comma list combinators
///   over an arbitrary element parser.
public final class Parsers {
    private Parsers() {}

    // ── phrase() — template-driven oracle-text phrase parser ──────────

    /// Parse an oracle-text phrase written in a small template DSL.
    /// Returns a parser that emits the **actually matched text** —
    /// the casing, inflection, and alternation branch the input took
    /// round-trip through the result, joined with single spaces
    /// between word tokens and no leading space before punctuation.
    ///
    /// Syntax:
    /// - `Word` (first token only, leading uppercase) → case-insensitive
    ///   match for `word` at sentence start.
    /// - `word` (anywhere else, or first token lowercase) → case-sensitive
    ///   match for the exact word.
    /// - `word(suffix)` → matches `wordsuffix` or `word`. Common uses:
    ///   `(s)` for regular plurals, `(es)` for plurals of words ending
    ///   in -s/-sh/-ch/-x, `(ing)` / `(ed)` for verb inflections.
    /// - `[alt1|alt2|…]` → alternatives (required); each alt is itself
    ///   a sub-template.
    /// - `[tok1 tok2 …]` → a single alt that's a required multi-token
    ///   sub-template.
    /// - `word?`, `[…]?` → optional suffix on the preceding token.
    /// - `,`, `.`, `:` → basic sentence punctuation matched literally.
    ///
    /// The first token of the phrase cannot be optional, and neither
    /// can the first token of any bracket alt — `optionallyPrecededBy`
    /// doesn't exist in dot-parse. If you need the whole phrase to be
    /// optional at a call site, use `optionallyFollowedBy` on the
    /// parser that precedes it.
    public static Parser<String> phrase(String template) {
        try {
            return PHRASE_GRAMMAR.parseSkipping(CharPredicate.is(' '), template);
        } catch (RuntimeException e) {
            if (e.getCause() instanceof IllegalArgumentException iae) throw iae;
            throw new IllegalArgumentException("invalid phrase: \"" + template + "\": " + e.getMessage(), e);
        }
    }

    // ── List combinators ──────────────────────────────────────────────

    /// Oxford-comma list with "and": `A`, `A and B`, `A, B, and C`.
    public static <T> Parser<List<T>> andList(Parser<T> element) {
        return list(element, word("and"));
    }

    /// Oxford-comma list with "or": `A`, `A or B`, `A, B, or C`.
    public static <T> Parser<List<T>> orList(Parser<T> element) {
        return list(element, word("or"));
    }

    /// Two-element list with "and/or": `A` or `A and/or B`.
    public static <T> Parser<List<T>> andOrList(Parser<T> element) {
        return list(element, string("and/or"));
    }

    /// Parses an Oxford-comma list of `element` joined by `connector`.
    private static <T> Parser<List<T>> list(Parser<T> element, Parser<?> connector) {
        var threeOrMore = sequence(
                element.followedBy(",").atLeastOnce(),
                connector.then(element),
                (List<T> heads, T tail) -> append(heads, tail));
        var pair = sequence(element, connector.then(element), List::of);
        var single = element.map(List::of);
        return anyOf(threeOrMore, pair, single);
    }

    private static <T> List<T> append(List<T> heads, T tail) {
        var list = new ArrayList<>(heads);
        list.add(tail);
        return List.copyOf(list);
    }

    // ── CompiledToken — a template token compiled to matcher parsers ──

    private record CompiledToken(
            Parser<String> firstForm, Parser<String> subsequentForm, boolean optional, boolean punctuation) {
        CompiledToken asOptional(String mark) {
            return new CompiledToken(firstForm, subsequentForm, true, punctuation);
        }

        Parser<String> asFirst() {
            if (optional) {
                throw new IllegalArgumentException(
                        "phrase cannot start with an optional token; use optionallyFollowedBy on the preceding parser"
                                + " instead");
            }
            return firstForm;
        }
    }

    private static Parser<String> append(Parser<String> acc, CompiledToken next) {
        var sep = next.punctuation() ? "" : " ";
        return next.optional()
                ? acc.optionallyFollowedBy(next.subsequentForm(), (a, b) -> a + sep + b)
                : sequence(acc, next.subsequentForm(), (a, b) -> a + sep + b);
    }

    private static Parser<String> fold(List<CompiledToken> tokens, boolean asFirst) {
        var head = tokens.getFirst();
        if (head.optional()) {
            throw new IllegalArgumentException(
                    "phrase cannot start with an optional token; use optionallyFollowedBy on the preceding parser"
                            + " instead");
        }
        var acc = asFirst ? head.firstForm() : head.subsequentForm();
        for (int i = 1; i < tokens.size(); i++) acc = append(acc, tokens.get(i));
        return acc;
    }

    // ── Template grammar ──────────────────────────────────────────────

    private static final Parser<String> WORD_INFLECTION =
            consecutive(CharacterSet.charsIn("[A-Za-z]"), "inflection").immediatelyBetween("(", ")");

    private static final Parser<String> TEMPLATE_WORD = consecutive(
                    CharacterSet.charsIn("[A-Za-z0-9'-]"), "phrase word")
            .optionallyFollowedBy(WORD_INFLECTION, (w, inflection) -> w + "(" + inflection + ")");

    private static final Parser<CompiledToken> PLAIN_RULE =
            TEMPLATE_WORD.map(Parsers::compilePlain).optionallyFollowedBy(string("?"), CompiledToken::asOptional);

    private static final Parser<CompiledToken> PUNCT_RULE =
            anyOf(string(","), string("."), string(":")).map(Parsers::compilePunct);

    private static final Parser.Rule<CompiledToken> TOKEN_RULE = new Parser.Rule<>();

    private static final Parser<CompiledToken> BRACKET_RULE = TOKEN_RULE
            .atLeastOnce()
            .atLeastOnceDelimitedBy("|")
            .between("[", "]")
            .map(Parsers::compileBracket)
            .optionallyFollowedBy(string("?"), CompiledToken::asOptional);

    private static final Parser<Parser<String>> PHRASE_GRAMMAR =
            TOKEN_RULE.map(CompiledToken::asFirst).withPostfixes(TOKEN_RULE, Parsers::append);

    static {
        TOKEN_RULE.definedAs(anyOf(PUNCT_RULE, BRACKET_RULE, PLAIN_RULE));
    }

    // ── Compilation of individual tokens ──────────────────────────────

    private static CompiledToken compilePlain(String text) {
        var open = text.indexOf('(');
        String[] alts;
        if (open >= 0 && text.endsWith(")")) {
            var base = text.substring(0, open);
            var suffix = text.substring(open + 1, text.length() - 1);
            alts = new String[] {base + suffix, base};
        } else {
            alts = new String[] {text};
        }
        var exact = exactAny(alts);
        var first = Character.isUpperCase(text.charAt(0)) ? titleOrLowerAny(alts) : exact;
        return new CompiledToken(first, exact, false, false);
    }

    private static CompiledToken compileBracket(List<List<CompiledToken>> alts) {
        var first = alts.stream().map(alt -> fold(alt, true)).collect(or());
        var subseq = alts.stream().map(alt -> fold(alt, false)).collect(or());
        return new CompiledToken(first, subseq, false, false);
    }

    private static CompiledToken compilePunct(String s) {
        var p = string(s);
        return new CompiledToken(p, p, false, true);
    }

    private static Parser<String> titleOrLower(String text) {
        var lower = text.toLowerCase();
        var title = Character.toUpperCase(text.charAt(0)) + text.substring(1);
        if (lower.equals(title)) return word(lower);
        return anyOf(word(title), word(lower));
    }

    private static Parser<String> exactAny(String... alts) {
        return Arrays.stream(alts).map(Parser::word).collect(or());
    }

    private static Parser<String> titleOrLowerAny(String... alts) {
        return Arrays.stream(alts).map(Parsers::titleOrLower).collect(or());
    }
}
