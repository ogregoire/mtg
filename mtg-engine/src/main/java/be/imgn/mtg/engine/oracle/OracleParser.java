package be.imgn.mtg.engine.oracle;

import static be.imgn.mtg.engine.oracle.Words.anyCiSentence;
import static be.imgn.mtg.engine.oracle.Words.ciWords;
import static be.imgn.mtg.engine.oracle.Words.w;
import static com.google.common.labs.parse.Parser.anyOf;
import static com.google.common.labs.parse.Parser.quotedBy;
import static com.google.common.labs.parse.Parser.sequence;
import static com.google.common.labs.parse.Parser.string;
import static com.google.common.labs.parse.Parser.word;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.labs.parse.Parser;
import com.google.mu.util.CharPredicate;

import org.jspecify.annotations.Nullable;

/// Entry point for parsing MTG oracle text using dot-parse combinators.
public final class OracleParser {
    private OracleParser() {}

    /// Whitespace predicate used by {@link #parse}: spaces only, never
    /// newlines. Newlines are explicit paragraph separators in the grammar
    /// ({@link #ORACLE_TEXT}), so we must not accidentally skip them.
    private static final CharPredicate WHITESPACE = CharPredicate.is(' ');

    /// Forward-declared ability rule for mutually-recursive grammar. An
    /// {@link Effect.GainAbility} inside an effect may carry a quoted ability
    /// (e.g., Citanul Hierophants: {@code Creatures you control have "{T}:
    /// Add {G}."}), which in turn contains effects. {@code Parser.Rule} ties
    /// that knot via {@link Parser.Rule#definedAs(Parser)} at the bottom of
    /// this file.
    public static final Parser.Rule<Ability> ABILITY = new Parser.Rule<>();

    // ── Reminder text (rule 207.2) — parenthesized flavor that does nothing ─

    /// Parser for a single `(...)` reminder-text block. Consumes the contents
    /// and discards them (the result is ignored wherever this is attached).
    static final Parser<String> REMINDER = quotedBy('(', ')');

    private static <T> Parser<T> withReminder(Parser<T> parser) {
        // Oracle punctuation around reminder text varies:
        //   "ability. (Reminder.)" — period before reminder.
        //   "ability (reminder)."  — period after reminder (e.g., Blorbian Buddy).
        // Absorb an optional period on either side of the optional reminder.
        return parser.optionallyFollowedBy(".")
                .optionallyFollowedBy(REMINDER, (t, r) -> t)
                .optionallyFollowedBy(".");
    }

    // ── Ability words (rule 207.2c) ────────────────────────────────────

    /// Closed list of ability words from rule 207.2c. They're flavor labels
    /// with no rules meaning; the parser consumes and discards them.
    private static final Parser<String> ABILITY_WORD_LABEL = anyCiSentence(
            "council's dilemma",
            "fathomless descent",
            "fateful hour",
            "join forces",
            "pack tactics",
            "secret council",
            "spell mastery",
            "tempting offer",
            "will of the council",
            "descend 4",
            "descend 8",
            "adamant",
            "addendum",
            "alliance",
            "battalion",
            "bloodrush",
            "celebration",
            "channel",
            "chroma",
            "cohort",
            "constellation",
            "converge",
            "coven",
            "delirium",
            "disappear",
            "domain",
            "eerie",
            "eminence",
            "enrage",
            "ferocious",
            "flurry",
            "formidable",
            "grandeur",
            "hellbent",
            "heroic",
            "imprint",
            "inspired",
            "kinship",
            "landfall",
            "lieutenant",
            "magecraft",
            "metalcraft",
            "morbid",
            "paradox",
            "parley",
            "radiance",
            "raid",
            "rally",
            "renew",
            "revolt",
            "strive",
            "survival",
            "sweep",
            "threshold",
            "undergrowth",
            "valiant",
            "vivid",
            "void");

    /// Small closed set of English connectives that may appear lowercase
    /// inside an otherwise title-cased ability-word label (e.g., "Sleight
    /// **of** Hand", "Will **of the** Council"). Anything else must be
    /// capitalized.
    private static final Set<String> CONNECTIVES = Set.of("of", "the", "and", "to", "in");

    private static boolean isAbilityWordToken(String s) {
        return !s.isEmpty() && (Character.isUpperCase(s.charAt(0)) || CONNECTIVES.contains(s));
    }

    /// Fallback ability-word label — 2–3 words not in the closed rule-book
    /// list, with the leading word capitalized. Bounded to multi-word labels
    /// so sentence-starting capitalizations ("If", "When") don't masquerade
    /// as ability words; single-word ability words are all covered by the
    /// known list above. Trailing words must be either capitalized or one
    /// of {@link #CONNECTIVES} (e.g., "Sleight of Hand", "Fear Gas").
    private static final Parser<String> CUSTOM_ABILITY_WORD = sequence(
            word().suchThat(s -> !s.isEmpty() && Character.isUpperCase(s.charAt(0)), "capitalized ability-word"),
            word().suchThat(OracleParser::isAbilityWordToken, "ability-word token")
                    .atLeastOnce()
                    .suchThat(ws -> ws.size() <= 2, "at most 2 trailing words"),
            (first, tail) -> first + " " + String.join(" ", tail));

    /// "<ability-word> — " prefix (rule 207.2c). Known labels are tried
    /// first; the capitalized fallback handles set-specific custom labels.
    private static final Parser<String> ABILITY_WORD_PREFIX =
            anyOf(ABILITY_WORD_LABEL, CUSTOM_ABILITY_WORD).followedBy(string("—"));

    private static <T> Parser<T> withAbilityWord(Parser<T> parser) {
        return anyOf(ABILITY_WORD_PREFIX.then(parser), parser);
    }

    // ── Triggered ability ──────────────────────────────────────────────

    /// Sequence of effects joined by ".", ", then", "then", or ",".
    /// Rule 608: oracle text often chains multiple effects in a single sentence
    /// or across sentences; each is a separate effect. The `may … . If you/they
    /// do, …` idiom is already collapsed at parse time by
    /// {@link EffectParsers#MAY_DRAW} and friends, so no post-processing is
    /// needed here.
    private static final Parser<List<Effect>> EFFECT_SEQUENCE = EffectParsers.EFFECT.atLeastOnceDelimitedBy(
            anyOf(ciWords(", then"), w("then"), w("and"), string("."), string(",")), Collectors.toUnmodifiableList());

    static final Parser<Ability> TRIGGERED = withReminder(withAbilityWord(sequence(
            anyOf(w("when"), w("whenever"), w("at")),
            TriggerEventParsers.TRIGGER_EVENT.followedBy(string(",")),
            EFFECT_SEQUENCE,
            (trigger, event, effects) -> new Ability.TriggeredAbility(trigger, event, null, effects))));

    // ── Activated ability: cost : effects ───────────────────────────────

    static final Parser<Ability> ACTIVATED = withReminder(withAbilityWord(sequence(
            CostParsers.COST_EXPRESSION.followedBy(string(":")), EFFECT_SEQUENCE, Ability.ActivatedAbility::new)));

    // ── Spell ability: just effects ────────────────────────────────────

    static final Parser<Ability> SPELL = withReminder(withAbilityWord(EFFECT_SEQUENCE.map(Ability.SpellAbility::new)));

    // ── Tie the recursive knot (rule ABILITY) ──────────────────────────
    // ACTIVATED is tried first because it requires a colon, TRIGGERED next
    // since it needs "when"/"whenever"/"at" and a comma, then the keyword
    // list (so flat "flying" still works inside quoted abilities), and SPELL
    // last as the catch-all effect sequence.
    static {
        ABILITY.definedAs(anyOf(
                ACTIVATED,
                TRIGGERED,
                KeywordParsers.KEYWORD_LIST
                        .suchThat(l -> l.size() == 1, "single keyword")
                        .map(List::getFirst),
                SPELL));
    }

    // ── Standalone reminder text ────────────────────────────────────────

    /// A paragraph consisting only of reminder text (e.g., basic-land-like
    /// cards whose oracle text is `({T}: Add {G}.)`). Produces no abilities.
    private static final Parser<List<Ability>> REMINDER_ONLY =
            REMINDER.atLeastOnce().thenReturn(List.of());

    /// One oracle-text paragraph: reminder-only, activated, triggered,
    /// spell (with their natural trailing period absorbed by
    /// {@link #withReminder}), or a keyword list (no trailing period — MTG
    /// convention). {@code SPELL} precedes {@code KEYWORD_LIST} so ability-
    /// word prefixes like "Fear Gas — …" are consumed as a spell ability
    /// rather than a partial keyword match ("Fear").
    private static final Parser<List<Ability>> PARAGRAPH = anyOf(
            REMINDER_ONLY,
            ACTIVATED.map(List::of),
            TRIGGERED.map(List::of),
            SPELL.map(List::of),
            // Keyword lines usually have no terminal period, but parameterized
            // keywords like `Equip—Discard a card.` do (Murderer's Axe).
            KeywordParsers.KEYWORD_LIST.optionallyFollowedBy("."));

    /// Full oracle text: paragraphs separated by one-or-more newlines,
    /// flattened. Newlines are explicit delimiters (the skip predicate for
    /// {@link #parse} is spaces only), and each paragraph arm consumes its
    /// own punctuation, so no string preprocessing is needed beyond
    /// self-reference substitution. Multiple newlines (blank lines between
    /// paragraphs) are allowed.
    private static final Parser<?> PARAGRAPH_SEP = string("\n").atLeastOnce();

    private static final Parser<List<Ability>> ORACLE_TEXT = PARAGRAPH.atLeastOnceDelimitedBy(
            PARAGRAPH_SEP, Collectors.flatMapping(List::stream, Collectors.toUnmodifiableList()));

    /// Parse oracle text into a flat list of abilities. The only
    /// preprocessing is self-reference substitution — the card's printed
    /// name (and its legendary short name, if any) is replaced with `~`
    /// before parsing. Everything else — paragraph structure, sentence
    /// punctuation, reminder text — is handled by {@link #ORACLE_TEXT}.
    public static List<Ability> parse(String cardName, String oracleText) {
        if (oracleText == null || oracleText.isBlank()) return List.of();
        var normalized = oracleText.replace(cardName, "~");
        var shortName = legendaryShortName(cardName);
        if (shortName != null) {
            normalized = normalized.replace(shortName, "~");
        }
        return ORACLE_TEXT.parseSkipping(WHITESPACE, normalized);
    }

    /// Best-effort short name for a legendary card: the part before a comma
    /// ("Silvos, Rogue Elemental" → "Silvos") or the first word of the name
    /// when it has a sentence-like form ("Eron the Relentless" → "Eron").
    /// Returns {@code null} if no safe short-name is available.
    private static @Nullable String legendaryShortName(String cardName) {
        var comma = cardName.indexOf(',');
        if (comma > 2) {
            return cardName.substring(0, comma);
        }
        var space = cardName.indexOf(' ');
        if (space <= 2) return null;
        var first = cardName.substring(0, space);
        // Skip common articles to avoid replacing them across unrelated oracle text.
        if (first.equalsIgnoreCase("The") || first.equalsIgnoreCase("A") || first.equalsIgnoreCase("An")) {
            return null;
        }
        // Require the remainder to contain "the"/"of"/"and" to hint at a
        // legendary epithet (e.g., "Eron the Relentless", "Lord of the Pit").
        var rest = cardName.substring(space + 1).toLowerCase();
        if (rest.contains(" of ") || rest.startsWith("of ") || rest.contains(" the ") || rest.startsWith("the ")) {
            return first;
        }
        return null;
    }
}
