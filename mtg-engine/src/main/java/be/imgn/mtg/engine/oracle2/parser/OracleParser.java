package be.imgn.mtg.engine.oracle2.parser;

import static com.google.common.labs.parse.Parser.string;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.google.common.labs.parse.Parser;
import com.google.mu.util.CharPredicate;
import com.google.mu.util.Substring;

import org.jspecify.annotations.Nullable;

import be.imgn.mtg.engine.oracle2.domain.Ability;
import be.imgn.mtg.engine.oracle2.domain.ArtifactType;
import be.imgn.mtg.engine.oracle2.domain.BasicLandType;
import be.imgn.mtg.engine.oracle2.domain.BattleType;
import be.imgn.mtg.engine.oracle2.domain.CreatureType;
import be.imgn.mtg.engine.oracle2.domain.EnchantmentType;
import be.imgn.mtg.engine.oracle2.domain.NonBasicLandType;
import be.imgn.mtg.engine.oracle2.domain.PlaneswalkerType;
import be.imgn.mtg.engine.oracle2.domain.SpellType;

/// Entry point for parsing MTG oracle text using the oracle2
/// parser tree. Mirrors `oracle.parser.OracleParser` in shape: same
/// `parse(cardName, oracleText)` signature, same name-substitution
/// preprocessing, same vintage-friendly handling of legendary short
/// names and verb-name collisions.
///
/// **Coverage today**: dispatches to [AbilityParser#PARAGRAPH], so
/// cards parse into a flat [Ability] list — keyword paragraphs,
/// triggered abilities (`When|Whenever|At [event], [effects].`),
/// activated abilities (`[cost]: [effects].`), and bare spell
/// abilities (instant/sorcery effect bodies). Replacement effects,
/// conditional clauses, and reminder text aren't modelled yet —
/// cards using them will fail. The success/failure shape is the
/// only contract callers depend on (the tooling parses each face,
/// records success in `oracle_parsed2`, and reports a coverage
/// percentage).
public final class OracleParser {
    private OracleParser() {}

    /// Whitespace predicate used by [#parse]: spaces only, never
    /// newlines. Newlines are explicit paragraph separators in oracle
    /// text, so we never accidentally skip them.
    private static final CharPredicate WHITESPACE = CharPredicate.is(' ');

    /// One or more newlines — paragraph boundary. Blank lines between
    /// paragraphs are tolerated.
    private static final Parser<?> PARAGRAPH_SEP = string("\n").atLeastOnce();

    /// Full oracle text: paragraphs delimited by [#PARAGRAPH_SEP],
    /// flattened into a single ordered ability list.
    private static final Parser<List<Ability>> ORACLE_TEXT = AbilityParser.PARAGRAPH.atLeastOnceDelimitedBy(
            PARAGRAPH_SEP, Collectors.flatMapping(List::stream, Collectors.toUnmodifiableList()));

    /// Parse oracle text into a flat list of abilities. The only
    /// preprocessing is self-reference substitution (the card's
    /// printed name and its legendary short name become `~`) and
    /// d20-table collapsing. Throws on any failed paragraph; the
    /// caller catches and records the failure.
    public static List<Ability> parse(String cardName, String oracleText) {
        if (oracleText == null || oracleText.isBlank()) return List.of();
        var normalized = substituteName(oracleText, cardName);
        var shortName = legendaryShortName(cardName);
        if (shortName != null) {
            normalized = substituteName(normalized, shortName);
        }
        normalized = collapseD20Table(normalized);
        return ORACLE_TEXT.parseSkipping(WHITESPACE, normalized);
    }

    /// d20-outcome tables span multiple lines in printed oracle text
    /// ("Roll a d20.\n1—9 | Scry 1.\n10—19 | Scry 2.")
    /// but the line breaks would split them across paragraph
    /// boundaries. Fold the rows back onto the "Roll a dN." line.
    private static final Pattern D20_ROW = Pattern.compile("\n(\\d+(?:[—-]\\d+)?\\s*\\|)", Pattern.MULTILINE);

    private static String collapseD20Table(String text) {
        return D20_ROW.matcher(text).replaceAll(" $1");
    }

    /// Word boundary following a card-type keyword. Used to detect
    /// when the card name is acting as a subtype reference rather
    /// than a self-reference (Assembly-Worker: "Target Assembly-
    /// Worker creature…"). In that case the substitution is skipped
    /// so the subtype parser can see the literal name.
    private static final Pattern TYPE_AFTER = Pattern.compile(
            "\\s+(creature|artifact|enchantment|instant|sorcery|land|planeswalker|battle|spell|permanent|card|token)s?\\b",
            Pattern.CASE_INSENSITIVE);

    /// Card names that collide with common effect verbs. When the
    /// oracle text uses the name as a verb (starts the sentence, is
    /// followed by "target" / mana symbol / other verb-object
    /// patterns), we skip the `~` substitution so the verb parser
    /// can see the literal word.
    private static final Set<String> VERB_NAMES = Set.of("Exile", "Sacrifice", "Destroy", "Counter");

    private static String substituteName(String text, String name) {
        var isVerbName = VERB_NAMES.contains(name);
        var out = new StringBuilder(text.length());
        int i = 0;
        while (i < text.length()) {
            var idx = text.indexOf(name, i);
            if (idx < 0) {
                out.append(text, i, text.length());
                break;
            }
            out.append(text, i, idx);
            var after = idx + name.length();
            var rest = text.substring(after);
            var m = TYPE_AFTER.matcher(rest);
            var asVerb = isVerbName && (idx == 0 || text.charAt(idx - 1) == '.' || text.charAt(idx - 1) == '\n');
            // "named …" clauses use the card's own name as a literal
            // token name. Keep the name as-is so the card-name parser
            // can capture it verbatim.
            var asLiteralName = isInNamedClause(text, idx);
            if (m.lookingAt() || asVerb || asLiteralName) {
                out.append(name);
            } else {
                out.append('~');
            }
            i = after;
        }
        return out.toString();
    }

    /// True when `idx` sits inside a "named …" clause — i.e.,
    /// somewhere in the same sentence between the most recent `.` /
    /// `\n` and `idx` the substring "named " appears.
    private static boolean isInNamedClause(String text, int idx) {
        int sentenceStart = 0;
        for (int j = idx - 1; j >= 0; j--) {
            char c = text.charAt(j);
            if (c == '.' || c == '\n') {
                sentenceStart = j + 1;
                break;
            }
        }
        return text.substring(sentenceStart, idx).contains("named ");
    }

    private static final Set<String> ARTICLE_SHORT_NAMES = Set.of("the", "a", "an");

    /// Everything before the first comma — picks up "Silvos" from
    /// "Silvos, Rogue Elemental".
    private static final Substring.Pattern BEFORE_FIRST_COMMA = Substring.before(Substring.first(','));

    /// First space in the name, used to split "Eron the Relentless"
    /// into `("Eron", "the Relentless")`.
    private static final Substring.Pattern FIRST_SPACE = Substring.first(' ');

    /// Tokens that mark a name as a "legendary epithet".
    private static final Substring.Pattern EPITHET_MARKER = Substring.prefix("the ")
            .or(Substring.prefix("of "))
            .or(Substring.first(" the "))
            .or(Substring.first(" of "));

    /// Best-effort short name for a legendary card.
    private static @Nullable String legendaryShortName(String cardName) {
        var beforeComma = BEFORE_FIRST_COMMA.from(cardName).filter(s -> s.length() > 2);
        return beforeComma.orElseGet(() -> FIRST_SPACE
                .split(cardName)
                .filter((first, _) -> first.length() > 2)
                .filter((first, _) -> !ARTICLE_SHORT_NAMES.contains(first.toLowerCase(Locale.ROOT)))
                .filter((first, _) -> !KNOWN_SUBTYPE_SHORT_NAMES.contains(first))
                .filter((_, rest) ->
                        EPITHET_MARKER.in(rest.toLowerCase(Locale.ROOT)).isPresent())
                .map((first, _) -> first)
                .orElse(null));
    }

    private static final Set<String> KNOWN_SUBTYPE_SHORT_NAMES = collectSubtypeShortNames();

    private static Set<String> collectSubtypeShortNames() {
        var names = new HashSet<String>();
        for (var sub : CreatureType.values()) addFirstName(names, sub.text());
        for (var sub : BasicLandType.values()) addFirstName(names, sub.text());
        for (var sub : NonBasicLandType.values()) addFirstName(names, sub.text());
        for (var sub : ArtifactType.values()) addFirstName(names, sub.text());
        for (var sub : EnchantmentType.values()) addFirstName(names, sub.text());
        for (var sub : SpellType.values()) addFirstName(names, sub.text());
        for (var sub : BattleType.values()) addFirstName(names, sub.text());
        for (var sub : PlaneswalkerType.values()) addFirstName(names, sub.text());
        return Set.copyOf(names);
    }

    private static void addFirstName(Set<String> out, String phraseText) {
        var paren = phraseText.indexOf('(');
        var bracket = phraseText.indexOf('[');
        var end = phraseText.length();
        if (paren > 0 && paren < end) end = paren;
        if (bracket > 0 && bracket < end) end = bracket;
        var head = phraseText.substring(0, end).trim();
        if (!head.isEmpty() && Character.isUpperCase(head.charAt(0))) {
            out.add(head);
        }
    }
}
