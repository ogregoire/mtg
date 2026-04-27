package be.imgn.mtg.engine.oracle.parser;

import static be.imgn.mtg.engine.oracle.parser.Words.phrase;
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
import com.google.mu.util.Substring;

import org.jspecify.annotations.Nullable;

import be.imgn.mtg.engine.oracle.domain.*;

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
    /// with no rules meaning; the parser consumes and discards them. The
    /// first alternative is capitalized so [Words#phrase] treats the
    /// whole bracket alternation as case-insensitive.
    private static final List<String> ABILITY_WORDS = List.of(
            "Council's dilemma",
            "Fathomless descent",
            "Fateful hour",
            "Join forces",
            "Pack tactics",
            "Secret council",
            "Spell mastery",
            "Tempting offer",
            "Will of the council",
            "Descend 4",
            "Descend 8",
            "Adamant",
            "Addendum",
            "Aegis of the Emperor",
            "Alliance",
            "Battalion",
            "Bloodrush",
            "Celebration",
            "Channel",
            "Chroma",
            "Cohort",
            "Constellation",
            "Converge",
            "Coven",
            "Delirium",
            "Disappear",
            "Domain",
            "Eerie",
            "Eminence",
            "Enrage",
            "Ferocious",
            "Flurry",
            "Formidable",
            "Grandeur",
            "Hellbent",
            "Heroic",
            "Imprint",
            "Inspired",
            "Kinship",
            "Landfall",
            "Lieutenant",
            "Magecraft",
            "Metalcraft",
            "Morbid",
            "Paradox",
            "Parley",
            "Radiance",
            "Raid",
            "Rally",
            "Renew",
            "Revolt",
            "Strive",
            "Survival",
            "Sweep",
            "Threshold",
            "Undergrowth",
            "Valiant",
            "Vivid",
            "Void");

    private static final Parser<String> ABILITY_WORD_LABEL =
            phrase(ABILITY_WORDS.stream().collect(Collectors.joining("|", "[", "]")));

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
    /// rejected downstream because no "—" follows. Also accepts a
    /// comma-grouped numeric label (Jumbo Cactuar: "10,000 Needles —
    /// …") since flavor words occasionally start with digits.
    private static final Parser<String> CAPITALIZED_WORD = anyOf(
            consecutive(CharacterSet.charsIn("[0-9,]"), "numeric label"),
            word().suchThat(s -> !s.isEmpty() && Character.isUpperCase(s.charAt(0)), "capitalized ability-word"));

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

    /// Trailing "Round up each time." / "Round down each time." directive
    /// that uniformly specializes every unspecialized [Amount.Half] in
    /// the enclosing effect list (Peer into the Abyss, Hydroid Krasis).
    /// The leading `.` separates the directive from the preceding
    /// sentence; the trailing period is left for [#withReminder] to
    /// absorb.
    private static final Parser<Amount.Rounding> ROUND_EACH_TIME = string(".")
            .then(phrase("Round"))
            .then(anyOf(
                    phrase("up each time").thenReturn(Amount.Rounding.UP),
                    phrase("down each time").thenReturn(Amount.Rounding.DOWN)));

    /// Sequence of effects joined by ".", ", then", "then", or ",".
    /// Rule 608: oracle text often chains multiple effects in a single sentence
    /// or across sentences; each is a separate effect. The `may … . If you/they
    /// do, …` idiom is already collapsed at parse time by
    /// [EffectParsers#MAY_DRAW] and friends, so no post-processing is
    /// needed here. A trailing "Round up/down each time." directive
    /// (if present) is folded onto every [Amount.Half] in the list
    /// via [EffectRounding#roundAmount].
    private static final Parser<List<Effect>> EFFECT_SEQUENCE = EffectParsers.CLAUSE
            .atLeastOnceDelimitedBy(
                    anyOf(
                            // Longer matches first so ". Then" wins over ".",
                            // and ", then" wins over either ",". "Then" is
                            // title-or-lower since it starts a new sentence;
                            // ", then" and a bare "then" are strictly lowercase
                            // — they sit mid-sentence.
                            sequence(string("."), phrase("Then"), (_, _) -> ". then"),
                            phrase(", then"),
                            phrase("then"),
                            phrase(", and"),
                            phrase("and"),
                            string("."),
                            string(",")),
                    Collectors.flatMapping(List::stream, Collectors.toUnmodifiableList()))
            .optionallyFollowedBy(ROUND_EACH_TIME, (effects, rounding) -> effects.stream()
                    .map(e -> AmountParsers.roundAmount(e, rounding))
                    .toList());

    /// One triggered line may yield multiple [Ability.TriggeredAbility]
    /// instances when the oracle text shares a subject across disjoint
    /// events ("when you scry or surveil, draw a card" — one ability per
    /// event; no composed trigger value is ever stored). An optional
    /// "if \[predicate\]," after the event's comma lands on each emitted
    /// ability's `interveningIf` slot (rule 603.4; Opal Lake Gatekeepers).
    private record IfAndEffects(@Nullable Condition iff, List<Effect> effects) {}

    /// Trigger-level "if \[condition\]," prefix — the rule 603.4
    /// intervening-if. Reuses the typed [EffectParsers#IF_PREFIX_CONDITION]
    /// so the trigger and effect-prefix paths agree on which condition
    /// shapes are recognized; the resulting [Condition] lands on the
    /// emitted [Ability.TriggeredAbility]'s `interveningIf` slot, which
    /// is what makes it a 603.4 condition (checked when the trigger
    /// goes on the stack and again on resolution).
    private static final Parser<IfAndEffects> IF_AND_EFFECTS = anyOf(
            sequence(EffectParsers.IF_PREFIX_CONDITION, EFFECT_SEQUENCE, IfAndEffects::new),
            EFFECT_SEQUENCE.map(effects -> new IfAndEffects(null, effects)));

    /// "This ability triggers only \[N times|once\] each turn." — caps
    /// the per-turn trigger count of the immediately preceding triggered
    /// ability (Mary Jane Watson). Per-turn is implicit, mirroring
    /// [Effect.ActivationLimit]. Returns the cap as an [Amount]. The
    /// leading `.` separates the directive from the preceding sentence;
    /// the trailing period is left for [#withReminder] to absorb.
    private static final Parser<Amount> TRIGGER_FREQUENCY_LIMIT = string(".")
            .then(phrase("This ability triggers only"))
            .then(anyOf(word("once").thenReturn(Amount.exact(1)), AmountParsers.AMOUNT.followedBy(phrase("time(s)"))))
            .followedBy(phrase("each turn"));

    static final Parser<List<Ability>> TRIGGERED = withReminder(withAbilityWord(sequence(
                    anyOf(
                            phrase("When").thenReturn("when"),
                            phrase("Whenever").thenReturn("whenever"),
                            phrase("At").thenReturn("at"),
                            // "As [subject] enters" — replacement-style ETB
                            // (rule 616, Sol Grail: "As this artifact enters,
                            // choose a color."). Treated as a trigger-word
                            // variant; the semantic distinction from
                            // "when … enters" is encoded by the oracle-side
                            // "as" marker alone.
                            phrase("As").thenReturn("as")),
                    TriggerEventParsers.TRIGGER_EVENT.followedBy(string(",")),
                    IF_AND_EFFECTS,
                    (trigger, events, body) -> events.stream()
                            .<Ability>map(ev -> new Ability.TriggeredAbility(trigger, ev, body.iff(), body.effects()))
                            .toList())
            .optionallyFollowedBy(TRIGGER_FREQUENCY_LIMIT, (abilities, limit) -> abilities.stream()
                    .<Ability>map(a -> ((Ability.TriggeredAbility) a).withTriggerLimit(limit))
                    .toList())));

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

    /// "Simultaneously, <effect-sequence>" — wraps the chain in a
    /// single [Effect.Simultaneously] so the engine resolves all
    /// effects at the same moment rather than sequentially (Time and
    /// Tide: "Simultaneously, all phased-out creatures phase in and
    /// all creatures with phasing phase out." — phased-in creatures
    /// don't get re-phased-out by the second clause).
    private static final Parser<List<Effect>> SIMULTANEOUSLY = phrase("Simultaneously")
            .followedBy(string(","))
            .then(EFFECT_SEQUENCE)
            .<List<Effect>>map(effects -> List.of(new Effect.Simultaneously(effects)));

    static final Parser<Ability> SPELL =
            withReminder(withAbilityWord(anyOf(SIMULTANEOUSLY, EFFECT_SEQUENCE).map(Ability.SpellAbility::new)));

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
    static final Parser<Ability.Mode> MODE = string("•")
            .then(EffectParsers.EFFECT.atLeastOnceDelimitedBy(
                    anyOf(phrase(", then"), phrase("then"), phrase("and"), string(",")),
                    Collectors.toUnmodifiableList()))
            .followedBy(string("."))
            .map(effects -> new Ability.Mode(null, effects));

    /// "Choose [quantity] — \n• …\n• …" — modal spell (rule 700.2). Each
    /// mode is a bullet line with its own effect sequence (Aether Shockwave:
    /// "Choose one — • Tap all Spirits. • Tap all non-Spirit creatures.").
    /// Consumes the newlines that separate the mode bullets so the outer
    /// [#ORACLE_TEXT] paragraph splitter sees the entire modal block
    /// as a single paragraph.
    static final Parser<Ability> MODAL = withReminder(sequence(
            phrase("Choose").then(CHOOSE_QUANTITY).followedBy(string("—")),
            string("\n").then(MODE).atLeastOnce(),
            Ability.Modal::new));

    // ── Tie the recursive knot (rule ABILITY) ──────────────────────────
    // ACTIVATED is tried first because it requires a colon, TRIGGERED next
    // since it needs "when"/"whenever"/"at" and a comma, then the keyword
    // list (so flat "flying" still works inside quoted abilities), and SPELL
    // last as the catch-all effect sequence.
    static {
        ABILITY.definedAs(anyOf(
                ACTIVATED,
                TRIGGERED
                        .suchThat(l -> l.size() == 1, "single triggered ability")
                        .map(List::getFirst),
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

    private static final Parser<Ability> CASTING_MODIFIER = withAbilityWord(phrase("Cast this spell only")
                    .then(MODIFIER_WORD.atLeastOnce().map(words -> String.join(" ", words)))
                    .<Ability>map(text -> new Ability.CastingModifier("only " + text)))
            .optionallyFollowedBy(".");

    /// One ability arm — used by [#PARAGRAPH] which chains
    /// `.atLeastOnce()` so a single paragraph can carry multiple
    /// abilities separated by sentence punctuation (Mirage Mesa /
    /// Crossroads Village / Uncharted Haven: "This land enters
    /// tapped. As it enters, choose a color." emits SpellAbility +
    /// TriggeredAbility from one paragraph).
    private static final Parser<List<Ability>> ABILITY_ONE = anyOf(
            REMINDER_ONLY,
            MODAL.map(List::of), // must precede SPELL (starts with "Choose" which SPELL could swallow)
            ACTIVATED.map(List::of),
            TRIGGERED,
            CASTING_MODIFIER.map(List::of), // must precede SPELL (starts with "Cast")
            SPELL.map(List::of),
            // Keyword lines usually have no terminal period, but parameterized
            // keywords like `Equip—Discard a card.` do (Murderer's Axe).
            KeywordParsers.KEYWORD_LIST.optionallyFollowedBy(".").optionallyFollowedBy(REMINDER, (l, _) -> l));

    private static final Parser<List<Ability>> PARAGRAPH = ABILITY_ONE
            .atLeastOnce()
            .map(lists -> lists.stream().flatMap(List::stream).toList());

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
        normalized = collapseD20Table(normalized);
        return ORACLE_TEXT.parseSkipping(WHITESPACE, normalized);
    }

    /// d20-outcome tables span multiple lines in printed oracle text
    /// ("Roll a d20.\n1—9 | Scry 1.\n10—19 | Scry 2.\n20 | Scry 3.")
    /// but the line breaks would split them across [#PARAGRAPH] boundaries.
    /// Fold the rows back onto the "Roll a dN." line so a single
    /// paragraph parser can consume the whole table.
    private static final Pattern D20_ROW = Pattern.compile("\n(\\d+(?:[—-]\\d+)?\\s*\\|)", Pattern.MULTILINE);

    private static String collapseD20Table(String text) {
        return D20_ROW.matcher(text).replaceAll(" $1");
    }

    /// Word boundary following a card-type keyword. Used to detect when
    /// the card name is acting as a subtype reference rather than a
    /// self-reference (Assembly-Worker: "Target Assembly-Worker
    /// creature…"). In that case the substitution is skipped so the
    /// subtype parser can see the literal name.
    private static final Pattern TYPE_AFTER = Pattern.compile(
            "\\s+(creature|artifact|enchantment|instant|sorcery|land|planeswalker|battle|spell|permanent|card|token)s?\\b",
            Pattern.CASE_INSENSITIVE);

    /// Card names that collide with common effect verbs. When the oracle
    /// text uses the name as a verb (starts the sentence, is followed
    /// by "target" / mana symbol / other verb-object patterns), we
    /// skip the `~` substitution so the verb parser can see the
    /// literal word (Exile: "Exile target nonwhite attacking
    /// creature.").
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
            // token name (Kher Keep: "Create a … token named Kobolds
            // of Kher Keep."). Keep the name as-is so
            // [CardNameParsers#CARD_NAME] can capture it verbatim.
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

    /// True when `idx` sits inside a "named …" clause — i.e., somewhere
    /// in the same sentence between the most recent `.` / `\n` and
    /// `idx` the substring "named " appears. Used by
    /// [#substituteName] to keep the card's own name literal when
    /// it's the literal name of a token (Kher Keep) rather than a
    /// self-reference.
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

    /// First space in the name, used to split "Eron the Relentless" into
    /// `("Eron", "the Relentless")`.
    private static final Substring.Pattern FIRST_SPACE = Substring.first(' ');

    /// Tokens that mark a name as a "legendary epithet" (Eron **the**
    /// Relentless, Lord **of** the Pit): matched at word boundaries
    /// against the tail of the name.
    private static final Substring.Pattern EPITHET_MARKER = Substring.prefix("the ")
            .or(Substring.prefix("of "))
            .or(Substring.first(" the "))
            .or(Substring.first(" of "));

    /// Best-effort short name for a legendary card: the part before a comma
    /// ("Silvos, Rogue Elemental" → "Silvos") or the first word of the name
    /// when it has a sentence-like form ("Eron the Relentless" → "Eron").
    /// Returns `null` if no safe short-name is available.
    private static @Nullable String legendaryShortName(String cardName) {
        var beforeComma = BEFORE_FIRST_COMMA.from(cardName).filter(s -> s.length() > 2);
        return beforeComma.orElseGet(() -> FIRST_SPACE
                .split(cardName)
                .filter((first, _) -> first.length() > 2)
                .filter((first, _) -> !ARTICLE_SHORT_NAMES.contains(first.toLowerCase()))
                // Decline when the first word is itself a known subtype
                // ("Wall" in "Wall of Mulch") — references in oracle
                // text are to the subtype, not the card.
                .filter((first, _) -> !KNOWN_SUBTYPE_SHORT_NAMES.contains(first))
                .filter((_, rest) -> EPITHET_MARKER.in(rest.toLowerCase()).isPresent())
                .map((first, _) -> first)
                .orElse(null));
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
