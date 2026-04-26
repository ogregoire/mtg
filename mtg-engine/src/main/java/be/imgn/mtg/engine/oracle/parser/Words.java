package be.imgn.mtg.engine.oracle.parser;

import static com.google.common.labs.parse.Parser.anyOf;
import static com.google.common.labs.parse.Parser.consecutive;
import static com.google.common.labs.parse.Parser.or;
import static com.google.common.labs.parse.Parser.sequence;
import static com.google.common.labs.parse.Parser.string;
import static com.google.common.labs.parse.Parser.word;

import java.util.Arrays;
import java.util.List;

import com.google.common.labs.parse.CharacterSet;
import com.google.common.labs.parse.Parser;
import com.google.mu.util.CharPredicate;

/// Template-driven phrase parser for oracle text. The single entry
/// point, [#phrase(String)], compiles a small templating DSL into a
/// `Parser<String>` that matches the oracle-text rendering of that
/// template and **reconstructs the matched text** — for instance
/// `phrase("[a|b]")` matching `a` returns `"a"`, just like
/// `word("a").thenReturn("a")`. Multi-token phrases are joined with a
/// single space between word-like tokens; punctuation (`,`, `.`, `:`)
/// is concatenated without a preceding space.
final class Words {
    private Words() {}

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
    ///   a sub-template: it may contain multiple words with inflection,
    ///   nested brackets, punctuation, and optional `?` markers.
    /// - `[tok1 tok2 …]` → a single alt that's a required multi-token
    ///   sub-template.
    /// - `word?`, `[…]?` → optional suffix on the preceding token.
    /// - `,`, `.`, `:` → basic sentence punctuation matched literally
    ///   via `string(",")` / `string(".")` / `string(":")`.
    ///
    /// The first token of the phrase cannot be optional, and neither
    /// can the first token of any bracket alt — `optionallyPrecededBy`
    /// doesn't exist in dot-parse. If you need the whole phrase to be
    /// optional at a call site, use `optionallyFollowedBy` on the
    /// parser that precedes it.
    ///
    /// Implementation — the template is itself parsed by a
    /// `Parser<Parser<String>>` ([#PHRASE_GRAMMAR]). Each token rule
    /// directly emits the oracle-text matcher it represents, and the
    /// grammar folds them together with `sequence` /
    /// `optionallyFollowedBy` combiners that concatenate matched
    /// substrings. Brackets recurse via the forward-declared
    /// [#TOKEN_RULE], so their contents go through the exact same
    /// token grammar as the outer phrase.
    ///
    /// Examples:
    /// ```
    /// phrase("Destroy creature(s) you control")
    /// phrase("deal(s) [combat]? damage to")
    /// phrase("[Destroy|Exile] target creature(s) [is|are] blocked")
    /// phrase("Destroy [each [artifact|creature]]")
    /// ```
    static Parser<String> phrase(String template) {
        try {
            return PHRASE_GRAMMAR.parseSkipping(CharPredicate.is(' '), template);
        } catch (RuntimeException e) {
            if (e.getCause() instanceof IllegalArgumentException iae) throw iae;
            throw new IllegalArgumentException("invalid phrase: \"" + template + "\": " + e.getMessage(), e);
        }
    }

    // ── CompiledToken — a template token compiled to matcher parsers ──

    /// One template token, compiled to the two oracle-text matchers it
    /// may need: the `firstForm` applies when the token is the very
    /// first of the phrase (where capitalized plain words / brackets
    /// accept their sentence-start variant), and the `subsequentForm`
    /// applies at every other position (strictly case-sensitive). For
    /// lowercase-first tokens and for punctuation, the two forms are
    /// identical. `optional` records a trailing `?` in the template.
    /// `punctuation` flags `,`/`.`/`:` tokens so the fold concatenates
    /// them without a preceding space.
    private record CompiledToken(
            Parser<String> firstForm, Parser<String> subsequentForm, boolean optional, boolean punctuation) {
        /// Wither shape for `optionallyFollowedBy(string("?"), CompiledToken::asOptional)`.
        /// The `mark` argument is the consumed `?` token, unused.
        CompiledToken asOptional(String mark) {
            return new CompiledToken(firstForm, subsequentForm, true, punctuation);
        }

        /// Head-token entry: the matcher to use at position 0. Throws
        /// if the token is flagged optional — `optionallyPrecededBy`
        /// doesn't exist in dot-parse, so leading `?` has no meaning.
        Parser<String> asFirst() {
            if (optional) {
                throw new IllegalArgumentException(
                        "phrase cannot start with an optional token; use optionallyFollowedBy on the preceding parser"
                                + " instead");
            }
            return firstForm;
        }
    }

    /// Fold one subsequent token onto the running matcher. Word-like
    /// tokens are joined with a single space; punctuation sticks to
    /// the previous token.
    private static Parser<String> append(Parser<String> acc, CompiledToken next) {
        var sep = next.punctuation() ? "" : " ";
        return next.optional()
                ? acc.optionallyFollowedBy(next.subsequentForm(), (a, b) -> a + sep + b)
                : sequence(acc, next.subsequentForm(), (a, b) -> a + sep + b);
    }

    /// Fold a bracket-alt's token list into a single matcher. Applies
    /// the outer-position rule to the alt's head — if `asFirst` is
    /// true, the head's `firstForm` is used (title-or-lower for
    /// capitalized words/brackets); otherwise the head's
    /// `subsequentForm`. Throws if the head is flagged optional,
    /// mirroring [CompiledToken#asFirst].
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

    /// One template word — letters, digits, apostrophes, or dashes, with
    /// an optional `(suffix)` inflection marker. Common forms are
    /// `(s)` for regular plurals and `(es)` / `(ing)`
    /// / `(ed)` for other inflections.
    private static final Parser<String> WORD_INFLECTION =
            consecutive(CharacterSet.charsIn("[A-Za-z]"), "inflection").immediatelyBetween("(", ")");

    private static final Parser<String> TEMPLATE_WORD = consecutive(
                    CharacterSet.charsIn("[A-Za-z0-9'-]"), "phrase word")
            .optionallyFollowedBy(WORD_INFLECTION, (w, inflection) -> w + "(" + inflection + ")");

    /// Plain token: a word (possibly with an `(inflection)` marker),
    /// optionally flagged optional by a trailing `?`.
    private static final Parser<CompiledToken> PLAIN_RULE =
            TEMPLATE_WORD.map(Words::compilePlain).optionallyFollowedBy(string("?"), CompiledToken::asOptional);

    /// Punctuation token: a single `,`, `.`, or `:` matched literally.
    /// No `?` suffix — punctuation is always required.
    private static final Parser<CompiledToken> PUNCT_RULE =
            anyOf(string(","), string("."), string(":")).map(Words::compilePunct);

    /// Forward-declared recursive rule so [#BRACKET_RULE] can nest the
    /// full token grammar inside its own `[…]` body. Populated in the
    /// trailing `static {}` block once all three leaf rules exist.
    private static final Parser.Rule<CompiledToken> TOKEN_RULE = new Parser.Rule<>();

    /// Bracket token: `[alt1|alt2|…]` with optional trailing `?`.
    /// Each alt is itself a sub-phrase (one or more [#TOKEN_RULE]s),
    /// so brackets nest and accept inflection / punctuation / optional
    /// markers inside.
    private static final Parser<CompiledToken> BRACKET_RULE = TOKEN_RULE
            .atLeastOnce()
            .atLeastOnceDelimitedBy("|")
            .between("[", "]")
            .map(Words::compileBracket)
            .optionallyFollowedBy(string("?"), CompiledToken::asOptional);

    /// Template → oracle-text matcher. The first token provides the
    /// starting parser via [CompiledToken#asFirst]; every subsequent
    /// token is folded in via [#append].
    private static final Parser<Parser<String>> PHRASE_GRAMMAR =
            TOKEN_RULE.map(CompiledToken::asFirst).withPostfixes(TOKEN_RULE, Words::append);

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

    // ── Matcher helpers ───────────────────────────────────────────────

    /// Single-word title-or-lower match. Returns a one-arm `word(lower)`
    /// parser when title and lower coincide (e.g., `"X"`).
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
        return Arrays.stream(alts).map(Words::titleOrLower).collect(or());
    }
}
