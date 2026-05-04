package be.imgn.mtg.engine.oracle.parser;

import static com.google.common.labs.parse.Parser.anyOf;
import static com.google.common.labs.parse.Parser.consecutive;
import static com.google.common.labs.parse.Parser.sequence;
import static com.google.common.labs.parse.Parser.string;

import java.util.List;
import java.util.Set;

import com.google.common.labs.parse.CharacterSet;
import com.google.common.labs.parse.Parser;
import com.google.mu.util.CharPredicate;

/// Parsers for literal MTG card names appearing in oracle text. A card
/// name is a sequence of capitalized words optionally interspersed
/// with a small set of lowercase connectives (`of`, `the`, `and`, `in`,
/// `on`, `to`, `with`) and may carry a legendary-style epithet after a
/// comma (Toggo, Goblin Weaponsmith; Yuriko, the Tiger's Shadow). Used
/// by [TokenDescriptionParsers#TOKEN_NAME] for the "named \<card-name\>"
/// suffix on a token (Tooth and Claw: "named Carnivore"; Kher Keep:
/// "named Kobolds of Kher Keep") and available for any future site that
/// needs to capture a literal name verbatim.
public final class CardNameParsers {
    private CardNameParsers() {}

    /// Lowercase connectives that may appear between capitalized
    /// components of a card name. Covers English particles (Kobolds
    /// **of** Kher Keep; A Tale **for the** Ages; Azusa, Lost **but**
    /// Seeking; Descend **upon** the Sinful) and the small set of
    /// foreign-language nobiliary particles that show up in card
    /// names (Leonardo **da** Vinci; Bartolomé **del** Presidio;
    /// Aveline **de** Grandpré; Jacques **le** Vert; Gaius **van**
    /// Baelsar; Rasaad **yn** Bashir; Zenos **yae** Galvus; Pair
    /// **o'** Dice Lost). Anything not in this set and not starting
    /// with a non-lowercase character terminates the name.
    private static final Set<String> CONNECTIVES = Set.of(
            "a",
            "an",
            "and",
            "as",
            "at",
            "but",
            "by",
            "da",
            "de",
            "del",
            "for",
            "from",
            "in",
            "into",
            "la",
            "le",
            "of",
            "on",
            "or",
            "the",
            "to",
            "upon",
            "van",
            "with",
            "yae",
            "yn",
            // "o'" — Irish-style possessive contraction (Pair o' Dice
            // Lost). The apostrophe is included since the parseSkipping
            // tokenizer treats it as part of the same token.
            "o'");

    /// Permissive token character class: letters (ASCII + Latin
    /// Extended for accented forms), digits, apostrophes, hyphens, and
    /// `+`. Covers possessives (Yawgmoth's), hyphenated names (Yuan-Ti,
    /// Yore-Tiller), promo prefixes (A-Acererak), accented names
    /// (Séance, Surtr — Fiery Jötun, Khazad-dûm), and numeric/symbol-
    /// fronted names (1996 World Champion, +2 Mace).
    private static final CharPredicate NAME_CHAR =
            CharacterSet.charsIn("[A-Za-z0-9'+-]").or(CharPredicate.range('À', 'ſ'));

    private static final Parser<String> NAME_TOKEN = consecutive(NAME_CHAR, "name token");

    /// True when the token starts a card name — i.e., its first
    /// character is anything except a lowercase letter. Capitals,
    /// digits, and symbol-fronted names ("+2 Mace") all count.
    private static boolean isNameStart(String s) {
        return !s.isEmpty() && !Character.isLowerCase(s.charAt(0));
    }

    /// Token that can legitimately *end* a card name — a name-start
    /// (capital / digit / symbol) or a hyphen/apostrophe-bearing
    /// lowercase token (`il-Vec`, `l'Cie`). Distinct from
    /// [#isCardNameToken] in that bare connectives ("of", "in",
    /// "the") are excluded — a card name never legitimately ends on
    /// a connective, and consuming a trailing connective would steal
    /// the "in <zone>" suffix from a containing selector
    /// (Rite of Flame: "for each card named Rite of Flame in each
    /// graveyard.").
    private static boolean isNameEndToken(String s) {
        return isNameStart(s) || s.indexOf('-') > 0 || s.indexOf('\'') > 0;
    }

    private static final Parser<String> NAME_END_WORD =
            NAME_TOKEN.suchThat(CardNameParsers::isNameEndToken, "name-end token");

    private static final Parser<String> CONNECTIVE_WORD =
            NAME_TOKEN.suchThat(CONNECTIVES::contains, "card-name connective");

    /// `<Card Name>` — a literal MTG card name. Begins with a
    /// capitalized word; may extend with additional capitalized words,
    /// lowercase [#CONNECTIVES], and a legendary-style epithet
    /// introduced by a comma (Silvos, Rogue Elemental; Yuriko, the
    /// Tiger's Shadow). Returns the matched name verbatim with single-
    /// space separation between words and `, ` before an epithet.
    public static final Parser<String> CARD_NAME = NAME_TOKEN
            .suchThat(CardNameParsers::isNameStart, "name-start token")
            .withPostfixes(
                    anyOf(
                            // "// <name-start>" — double-faced card join
                            // ("Erdwal Illuminator // Erdwal Ripper").
                            // Tried first so the "//" wins.
                            string("//")
                                    .then(NAME_TOKEN.suchThat(CardNameParsers::isNameStart, "name-start token"))
                                    .map(tok -> " // " + tok),
                            // "& <name-end>" — ampersand-joined names.
                            string("&").then(NAME_END_WORD).map(tok -> " & " + tok),
                            // Trailing punctuation that prints flush
                            // against the preceding word — interjections
                            // ("Yip Yip!", "Continue?") and colon-led
                            // qualifiers ("Summon: Anima").
                            string("!"),
                            string("?"),
                            string(":"),
                            // ", <connective>* <name-end>" — comma-
                            // introduced epithet (Yuriko, the Tiger's
                            // Shadow). Connectives between the comma and
                            // the next name-end are pulled in atomically.
                            sequence(
                                    string(",")
                                            .then(CONNECTIVE_WORD.atLeastOnce().orElse(List.of())),
                                    NAME_END_WORD,
                                    (conns, end) ->
                                            conns.isEmpty() ? ", " + end : ", " + String.join(" ", conns) + " " + end),
                            // " <connective>+ <name-end>" — bridge over
                            // one or more connectives to the next name-
                            // end token (Lord of the Pit; Rest in
                            // Peace). Required to consume a name-end
                            // afterwards so we never end on a bare
                            // connective.
                            sequence(
                                    CONNECTIVE_WORD.atLeastOnce(),
                                    NAME_END_WORD,
                                    (conns, end) -> " " + String.join(" ", conns) + " " + end),
                            // " <name-end>" — bare name-end continuation.
                            NAME_END_WORD.map(tok -> " " + tok)),
                    String::concat);
}
