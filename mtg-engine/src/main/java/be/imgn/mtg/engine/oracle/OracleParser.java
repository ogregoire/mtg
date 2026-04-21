package be.imgn.mtg.engine.oracle;

import static be.imgn.mtg.engine.oracle.Words.anyCiSentence;
import static be.imgn.mtg.engine.oracle.Words.ciWords;
import static be.imgn.mtg.engine.oracle.Words.phrase;
import static be.imgn.mtg.engine.oracle.Words.w;
import static com.google.common.labs.parse.Parser.anyOf;
import static com.google.common.labs.parse.Parser.consecutive;
import static com.google.common.labs.parse.Parser.quotedBy;
import static com.google.common.labs.parse.Parser.sequence;
import static com.google.common.labs.parse.Parser.string;
import static com.google.common.labs.parse.Parser.word;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.google.common.labs.parse.CharacterSet;
import com.google.common.labs.parse.Parser;
import com.google.mu.util.CharPredicate;

import org.jspecify.annotations.Nullable;

/// Entry point for parsing MTG oracle text using dot-parse combinators.
public final class OracleParser {
    private OracleParser() {}

    /// Whitespace predicate used by [#parse]: spaces only, never
    /// newlines. Newlines are explicit paragraph separators in the grammar
    /// ([#ORACLE_TEXT]), so we must not accidentally skip them.
    private static final CharPredicate WHITESPACE = CharPredicate.is(' ');

    /// Forward-declared ability rule for mutually-recursive grammar. An
    /// [Effect.GainAbility] inside an effect may carry a quoted ability
    /// (e.g., Citanul Hierophants: `Creatures you control have "{T`:
    /// Add {G}."}), which in turn contains effects. `Parser.Rule` ties
    /// that knot via [Parser.Rule#definedAs(Parser)] at the bottom of
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

    /// Capitalized leading word for a label (ability-word 207.2c or
    /// flavor-word 207.2d). Names like "If" / "When" are admitted here but
    /// rejected downstream because no "—" follows.
    private static final Parser<String> CAPITALIZED_WORD =
            word().suchThat(s -> !s.isEmpty() && Character.isUpperCase(s.charAt(0)), "capitalized ability-word");

    /// Fallback label for ability-word (rule 207.2c) or flavor-word (rule
    /// 207.2d) prefixes: 1–3 words with the leading word capitalized, either
    /// a single label ("Protector") or a multi-word name ("Fear Gas",
    /// "Sleight of Hand"). Trailing words must be either capitalized or one
    /// of the [#CONNECTIVES]. The em-dash that follows (consumed by
    /// [#ABILITY_WORD_PREFIX]) is what distinguishes a label from an
    /// ordinary sentence-starting capital.
    private static final Parser<String> CUSTOM_ABILITY_WORD = anyOf(
            sequence(
                    CAPITALIZED_WORD,
                    word().suchThat(OracleParser::isAbilityWordToken, "ability-word token")
                            .atLeastOnce()
                            .suchThat(ws -> ws.size() <= 2, "at most 2 trailing words"),
                    (first, tail) -> first + " " + String.join(" ", tail)),
            CAPITALIZED_WORD);

    /// "<label> — " prefix consumed as flavor: either a known ability word
    /// (rule 207.2c) or a custom capitalized label covering both ability-
    /// word fallbacks and flavor words (rule 207.2d). The em-dash is what
    /// separates the label from the ability body.
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
    /// [EffectParsers#MAY_DRAW] and friends, so no post-processing is
    /// needed here.
    private static final Parser<List<Effect>> EFFECT_SEQUENCE = EffectParsers.CLAUSE.atLeastOnceDelimitedBy(
            anyOf(
                    // Longer matches first so ". Then" wins over ".", and
                    // ", then" wins over either ",".
                    sequence(string("."), w("then"), (_, _) -> ". then"),
                    ciWords(", then"),
                    w("then"),
                    w("and"),
                    string("."),
                    string(",")),
            Collectors.flatMapping(List::stream, Collectors.toUnmodifiableList()));

    static final Parser<Ability> TRIGGERED = withReminder(withAbilityWord(sequence(
            anyOf(w("when"), w("whenever"), w("at")),
            TriggerEventParsers.TRIGGER_EVENT.followedBy(string(",")),
            EFFECT_SEQUENCE,
            (trigger, event, effects) -> new Ability.TriggeredAbility(trigger, event, null, effects))));

    // ── Activated ability: cost : effects ───────────────────────────────

    /// "Any player may activate this ability \[but only …\]?." — permission
    /// modifier on an activated ability (rule 602.5e). Applied to the
    /// ActivatedAbility itself rather than contributing an effect. The
    /// preceding sentence-terminator `.` is consumed here so the
    /// paragraph boundary stays clean. An explicit `but only …` tail
    /// overrides the default of
    /// [Ability.AnyPlayerActivation.Unrestricted#UNRESTRICTED].
    private static final Parser<Ability.AnyPlayerActivation> ANY_PLAYER_ACTIVATION = string(".")
            .then(phrase("Any player may activate this ability"))
            .then(AnyPlayerActivationParsers.ACTIVATION.orElse(Ability.AnyPlayerActivation.Unrestricted.UNRESTRICTED));

    static final Parser<Ability> ACTIVATED = withReminder(withAbilityWord(sequence(
                    CostParsers.COST_EXPRESSION.followedBy(string(":")), EFFECT_SEQUENCE, Ability.ActivatedAbility::new)
            .optionallyFollowedBy(ANY_PLAYER_ACTIVATION, Ability.ActivatedAbility::withAnyPlayerActivation)
            .map(aa -> (Ability) aa)));

    // ── Spell ability: just effects ────────────────────────────────────

    static final Parser<Ability> SPELL = withReminder(withAbilityWord(EFFECT_SEQUENCE.map(Ability.SpellAbility::new)));

    // ── Modal ability ──────────────────────────────────────────────────

    /// Quantity phrase following "Choose": captures the verbatim text
    /// between "Choose" and the em-dash delimiter (e.g., "one", "one or
    /// both", "two", "X", "any number"). Bounded by "—" so it never spans
    /// into the mode bodies.
    private static final Parser<String> CHOOSE_QUANTITY = word().atLeastOnce()
            .suchThat(ws -> !ws.isEmpty(), "choose quantity")
            .map(ws -> String.join(" ", ws));

    /// One "• <effects>" bullet line of a modal spell. Effects are matched
    /// by [EffectParsers#EFFECT] chained on the same line delimiters
    /// the outer spell grammar uses, and the trailing sentence-terminator
    /// "." is consumed explicitly so modes don't run into each other.
    private static final Parser<Ability.Mode> MODE = string("•")
            .then(EffectParsers.EFFECT.atLeastOnceDelimitedBy(
                    anyOf(ciWords(", then"), w("then"), w("and"), string(",")), Collectors.toUnmodifiableList()))
            .followedBy(string("."))
            .map(effects -> new Ability.Mode(null, effects));

    /// "Choose [quantity] — \n• …\n• …" — modal spell (rule 700.2). Each
    /// mode is a bullet line with its own effect sequence (Aether Shockwave:
    /// "Choose one — • Tap all Spirits. • Tap all non-Spirit creatures.").
    /// Consumes the newlines that separate the mode bullets so the outer
    /// [#ORACLE_TEXT] paragraph splitter sees the entire modal block
    /// as a single paragraph.
    static final Parser<Ability> MODAL = withReminder(sequence(
            ciWords("choose").then(CHOOSE_QUANTITY).followedBy(string("—")),
            sequence(string("\n"), MODE, (_, m) -> m).atLeastOnce(),
            Ability.Modal::new));

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
    /// [#withReminder]), or a keyword list (no trailing period — MTG
    /// convention). `SPELL` precedes `KEYWORD_LIST` so ability-
    /// word prefixes like "Fear Gas — …" are consumed as a spell ability
    /// rather than a partial keyword match ("Fear").
    /// "Cast this spell only if/when/…" — a casting restriction that
    /// applies to the card's spell ability (rule 601.3). Captured as a
    /// [Ability.CastingModifier] carrying the verbatim predicate.
    /// Must precede SPELL so the leading "Cast" isn't parsed as a verb.
    /// Token allowing English contractions ("you've", "can't") — used by
    /// [#CASTING_MODIFIER] so predicates like "only if you've cast
    /// another spell this turn" round-trip.
    private static final Parser<String> MODIFIER_WORD =
            consecutive(CharacterSet.charsIn("[A-Za-z0-9'-]"), "modifier word");

    private static final Parser<Ability> CASTING_MODIFIER = withAbilityWord(ciWords("cast this spell only")
                    .then(MODIFIER_WORD.atLeastOnce().map(words -> String.join(" ", words)))
                    .<Ability>map(text -> new Ability.CastingModifier("only " + text)))
            .optionallyFollowedBy(".");

    private static final Parser<List<Ability>> PARAGRAPH = anyOf(
            REMINDER_ONLY,
            MODAL.map(List::of), // must precede SPELL (starts with "Choose" which SPELL could swallow)
            ACTIVATED.map(List::of),
            TRIGGERED.map(List::of),
            CASTING_MODIFIER.map(List::of), // must precede SPELL (starts with "Cast")
            SPELL.map(List::of),
            // Keyword lines usually have no terminal period, but parameterized
            // keywords like `Equip—Discard a card.` do (Murderer's Axe).
            KeywordParsers.KEYWORD_LIST.optionallyFollowedBy(".").optionallyFollowedBy(REMINDER, (l, _) -> l));

    /// Full oracle text: paragraphs separated by one-or-more newlines,
    /// flattened. Newlines are explicit delimiters (the skip predicate for
    /// [#parse] is spaces only), and each paragraph arm consumes its
    /// own punctuation, so no string preprocessing is needed beyond
    /// self-reference substitution. Multiple newlines (blank lines between
    /// paragraphs) are allowed.
    private static final Parser<?> PARAGRAPH_SEP = string("\n").atLeastOnce();

    private static final Parser<List<Ability>> ORACLE_TEXT = PARAGRAPH.atLeastOnceDelimitedBy(
            PARAGRAPH_SEP, Collectors.flatMapping(List::stream, Collectors.toUnmodifiableList()));

    /// Parse oracle text into a flat list of abilities. The only
    /// preprocessing is self-reference substitution — the card's printed
    /// name (and its legendary short name, if any) is replaced with `~`
    /// before parsing. When the name is immediately followed by a card-
    /// type word (e.g., Assembly-Worker: "Target Assembly-Worker
    /// creature"), the occurrence is left as-is so the type word can
    /// drive the subtype match instead. Everything else — paragraph
    /// structure, sentence punctuation, reminder text — is handled by
    /// [#ORACLE_TEXT].
    public static List<Ability> parse(String cardName, String oracleText) {
        if (oracleText == null || oracleText.isBlank()) return List.of();
        var normalized = substituteName(oracleText, cardName);
        var shortName = legendaryShortName(cardName);
        if (shortName != null) {
            normalized = substituteName(normalized, shortName);
        }
        return ORACLE_TEXT.parseSkipping(WHITESPACE, normalized);
    }

    /// Word boundary following a card-type keyword. Used to detect when
    /// the card name is acting as a subtype reference rather than a
    /// self-reference (Assembly-Worker: "Target Assembly-Worker
    /// creature…"). In that case the substitution is skipped so the
    /// subtype parser can see the literal name.
    private static final Pattern TYPE_AFTER = Pattern.compile(
            "\\s+(creature|artifact|enchantment|instant|sorcery|land|planeswalker|battle|spell|permanent|card|token)s?\\b",
            Pattern.CASE_INSENSITIVE);

    private static String substituteName(String text, String name) {
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
            if (m.lookingAt()) {
                out.append(name);
            } else {
                out.append('~');
            }
            i = after;
        }
        return out.toString();
    }

    /// Best-effort short name for a legendary card: the part before a comma
    /// ("Silvos, Rogue Elemental" → "Silvos") or the first word of the name
    /// when it has a sentence-like form ("Eron the Relentless" → "Eron").
    /// Returns `null` if no safe short-name is available.
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
        // If the first word is itself a known subtype ("Wall" in "Wall
        // of Mulch", "Serpent" in "Serpent of the Endless Sea"),
        // decline — references in oracle text are to the subtype, not
        // the card.
        if (KNOWN_SUBTYPE_SHORT_NAMES.contains(first)) {
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

    /// Set of subtype single-word names (creature types, land types,
    /// etc.) used to reject a first-word substitution candidate when
    /// the word is a known subtype rather than a legendary name.
    private static final Set<String> KNOWN_SUBTYPE_SHORT_NAMES = collectSubtypeShortNames();

    private static Set<String> collectSubtypeShortNames() {
        var names = new HashSet<String>();
        for (var sub : CreatureType.values()) addFirstName(names, sub.text());
        for (var sub : LandType.values()) addFirstName(names, sub.text());
        for (var sub : ArtifactType.values()) addFirstName(names, sub.text());
        for (var sub : EnchantmentType.values()) addFirstName(names, sub.text());
        for (var sub : SpellType.values()) addFirstName(names, sub.text());
        for (var sub : BattleType.values()) addFirstName(names, sub.text());
        for (var sub : PlaneswalkerType.values()) addFirstName(names, sub.text());
        return Set.copyOf(names);
    }

    private static void addFirstName(Set<String> out, String phraseText) {
        // The first token of a phrase template like "Wall(s)" or
        // "[Ally|Allies]" is the singular form we want to match as
        // a capitalized bare word.
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
