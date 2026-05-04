package be.imgn.mtg.engine.oracle2.parser;

import static com.google.common.labs.parse.Parser.anyOf;
import static com.google.common.labs.parse.Parser.consecutive;
import static com.google.common.labs.parse.Parser.sequence;
import static com.google.common.labs.parse.Parser.string;

import java.util.List;
import java.util.Set;

import com.google.common.labs.parse.CharacterSet;
import com.google.common.labs.parse.Parser;
import com.google.mu.util.CharPredicate;

/// Parser for literal MTG card names appearing in oracle text. A card
/// name is a sequence of capitalized words optionally interspersed
/// with a small set of lowercase connectives (`of`, `the`, `and`, `in`,
/// `on`, `to`, `with`, …) and may carry a legendary-style epithet
/// after a comma (Toggo, Goblin Weaponsmith; Yuriko, the Tiger's
/// Shadow). Returns the matched name verbatim with single-space
/// separation between words and `, ` before an epithet.
///
/// Self-contained — returns plain `String`, no `oracle2.domain`
/// coupling. Copied from the legacy `oracle.parser.CardNameParsers`
/// to keep `oracle2.parser` isolated from `oracle.parser`.
public final class CardNameParser {
    private CardNameParser() {}

    /// Lowercase connectives that may appear between capitalized
    /// components of a card name.
    private static final Set<String> CONNECTIVES = Set.of(
            "a", "an", "and", "as", "at", "but", "by", "da", "de", "del", "for", "from", "in", "into", "la", "le", "of",
            "on", "or", "the", "to", "upon", "van", "with", "yae", "yn", "o'");

    /// Permissive token character class: letters (ASCII + Latin
    /// Extended for accented forms), digits, apostrophes, hyphens, and
    /// `+`. Covers possessives, hyphenated names, accented names, and
    /// numeric/symbol-fronted names.
    private static final CharPredicate NAME_CHAR =
            CharacterSet.charsIn("[A-Za-z0-9'+-]").or(CharPredicate.range('À', 'ſ'));

    private static final Parser<String> NAME_TOKEN = consecutive(NAME_CHAR, "name token");

    private static boolean isNameStart(String s) {
        return !s.isEmpty() && !Character.isLowerCase(s.charAt(0));
    }

    private static boolean isNameEndToken(String s) {
        return isNameStart(s) || s.indexOf('-') > 0 || s.indexOf('\'') > 0;
    }

    private static final Parser<String> NAME_END_WORD =
            NAME_TOKEN.suchThat(CardNameParser::isNameEndToken, "name-end token");

    private static final Parser<String> CONNECTIVE_WORD =
            NAME_TOKEN.suchThat(CONNECTIVES::contains, "card-name connective");

    /// `<Card Name>` — a literal MTG card name. Begins with a
    /// capitalized word; may extend with additional capitalized words,
    /// lowercase [#CONNECTIVES], and a legendary-style epithet
    /// introduced by a comma. Returns the matched name verbatim with
    /// single-space separation and `, ` before an epithet.
    public static final Parser<String> CARD_NAME = NAME_TOKEN
            .suchThat(CardNameParser::isNameStart, "name-start token")
            .withPostfixes(
                    anyOf(
                            // "// <name-start>" — double-faced card join.
                            string("//")
                                    .then(NAME_TOKEN.suchThat(CardNameParser::isNameStart, "name-start token"))
                                    .map(tok -> " // " + tok),
                            // "& <name-end>" — ampersand-joined names.
                            string("&").then(NAME_END_WORD).map(tok -> " & " + tok),
                            // Trailing flush punctuation.
                            string("!"),
                            string("?"),
                            string(":"),
                            // ", <connective>* <name-end>" — comma-introduced epithet.
                            sequence(
                                    string(",")
                                            .then(CONNECTIVE_WORD.atLeastOnce().orElse(List.of())),
                                    NAME_END_WORD,
                                    (conns, end) ->
                                            conns.isEmpty() ? ", " + end : ", " + String.join(" ", conns) + " " + end),
                            // " <connective>+ <name-end>" — bridge connectives to next name-end.
                            sequence(
                                    CONNECTIVE_WORD.atLeastOnce(),
                                    NAME_END_WORD,
                                    (conns, end) -> " " + String.join(" ", conns) + " " + end),
                            // " <name-end>" — bare name-end continuation.
                            NAME_END_WORD.map(tok -> " " + tok)),
                    String::concat);
}
