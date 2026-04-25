package be.imgn.mtg.engine.oracle.parser;

import static com.google.common.labs.parse.Parser.anyOf;
import static com.google.common.labs.parse.Parser.consecutive;
import static com.google.common.labs.parse.Parser.string;

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

    private static boolean isCardNameToken(String s) {
        // Hyphenated lowercase tokens like "il-Vec", "en-Kor", "il-Dal",
        // "il-Kor", "en-Vec", "en-Dal", "bin-Kroog" — Mirage / Onslaught
        // tribal suffixes that follow a noun. Apostrophe-prefixed
        // lowercase tokens like "l'Cie" (Final Fantasy XIII) and
        // "de'Arnise" (Baldur's Gate). Accept any token containing a
        // hyphen or apostrophe so these chain after the head noun.
        return isNameStart(s) || CONNECTIVES.contains(s) || s.indexOf('-') > 0 || s.indexOf('\'') > 0;
    }

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
                            // ("Erdwal Illuminator // Erdwal Ripper";
                            // "A-Alrund, God of the Cosmos // A-Hakka,
                            // Whispering Raven"). Tried first so the
                            // "//" wins over a stray name token.
                            string("//")
                                    .then(NAME_TOKEN.suchThat(CardNameParsers::isNameStart, "name-start token"))
                                    .map(tok -> " // " + tok),
                            // "& <name-start>" — ampersand-joined names
                            // ("Tokka & Rahzar"; "Splinter & Leo,
                            // Father & Son").
                            string("&")
                                    .then(NAME_TOKEN.suchThat(CardNameParsers::isCardNameToken, "card-name token"))
                                    .map(tok -> " & " + tok),
                            // Trailing punctuation that prints flush
                            // against the preceding word — interjections
                            // ("Yip Yip!", "Continue?") and colon-led
                            // qualifiers ("Summon: Anima", "Vault 11:
                            // Voter's Dilemma", "Circle of Protection:
                            // Red"). Each appends without an extra space
                            // so the round-trip output matches the
                            // printed name.
                            string("!"),
                            string("?"),
                            string(":"),
                            // ", <name-token>" — comma-introduced epithet
                            // segment (legendary cards). The leading
                            // comma is consumed here; subsequent name
                            // tokens chain via the bare-word arm.
                            string(",")
                                    .then(NAME_TOKEN.suchThat(CardNameParsers::isCardNameToken, "card-name token"))
                                    .map(tok -> ", " + tok),
                            // " <name-token>" — additional name-start
                            // token (capital, digit, or symbol-fronted)
                            // or lowercase connective. The space is
                            // supplied by parseSkipping; we add it back
                            // when reassembling the captured string.
                            NAME_TOKEN
                                    .suchThat(CardNameParsers::isCardNameToken, "card-name token")
                                    .map(tok -> " " + tok)),
                    String::concat);
}
