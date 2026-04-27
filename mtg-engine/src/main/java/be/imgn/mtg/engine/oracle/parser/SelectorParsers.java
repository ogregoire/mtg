package be.imgn.mtg.engine.oracle.parser;

import static be.imgn.mtg.engine.oracle.parser.Words.phrase;
import static com.google.common.labs.parse.Parser.anyOf;
import static com.google.common.labs.parse.Parser.consecutive;
import static com.google.common.labs.parse.Parser.or;
import static com.google.common.labs.parse.Parser.sequence;
import static com.google.common.labs.parse.Parser.string;
import static com.google.common.labs.parse.Parser.word;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.labs.parse.CharacterSet;
import com.google.common.labs.parse.Parser;

import org.jspecify.annotations.Nullable;

import be.imgn.mtg.engine.oracle.domain.*;

/// Parsers for selectors, types, amounts, and related noun-phrase grammar.
final class SelectorParsers {
    private SelectorParsers() {}

    // ── Primitive parsers ──────────────────────────────────────────────

    /// Re-exposed from [AmountParsers] so existing static-imports via
    /// `SelectorParsers.INTEGER` keep working. Prefer the canonical
    /// home in [AmountParsers] for new uses.
    static final Parser<Integer> INTEGER = AmountParsers.INTEGER;

    static final Parser<Integer> SIGNED_INT = AmountParsers.SIGNED_INT;

    static final Parser<Integer> WORD_NUMBER = AmountParsers.WORD_NUMBER;

    public static final Parser<Integer> NUMBER = AmountParsers.NUMBER;

    public static final Parser<Amount> AMOUNT = AmountParsers.AMOUNT;

    // ── Enums ──────────────────────────────────────────────────────────

    public static final Parser<Color> COLOR = anyOf(
            phrase("White").thenReturn(Color.WHITE),
            phrase("Blue").thenReturn(Color.BLUE),
            phrase("Black").thenReturn(Color.BLACK),
            phrase("Red").thenReturn(Color.RED),
            phrase("Green").thenReturn(Color.GREEN));

    public static final Parser<CardType> CARD_TYPE = anyOf(
            phrase("Creature(s)").thenReturn(CardType.CREATURE),
            phrase("Artifact(s)").thenReturn(CardType.ARTIFACT),
            phrase("Enchantment(s)").thenReturn(CardType.ENCHANTMENT),
            phrase("Land(s)").thenReturn(CardType.LAND),
            phrase("Planeswalker(s)").thenReturn(CardType.PLANESWALKER),
            phrase("Battle(s)").thenReturn(CardType.BATTLE),
            phrase("Instant(s)").thenReturn(CardType.INSTANT),
            anyOf(phrase("Sorcery"), phrase("Sorceries")).thenReturn(CardType.SORCERY),
            phrase("Kindred").thenReturn(CardType.KINDRED),
            phrase("Dungeon(s)").thenReturn(CardType.DUNGEON));

    public static final Parser<GameObjectType> GAME_OBJECT_TYPE = anyOf(
            phrase("Permanent(s)").thenReturn(GameObjectType.PERMANENT),
            phrase("Spell(s)").thenReturn(GameObjectType.SPELL),
            phrase("Card(s)").thenReturn(GameObjectType.CARD),
            phrase("Token(s)").thenReturn(GameObjectType.TOKEN),
            phrase("Source(s)").thenReturn(GameObjectType.SOURCE),
            anyOf(phrase("Ability"), phrase("Abilities")).thenReturn(GameObjectType.ABILITY),
            phrase("Player(s)").thenReturn(GameObjectType.PLAYER));

    public static final Parser<Supertype> SUPERTYPE = anyOf(
            phrase("Legendary").thenReturn(Supertype.LEGENDARY),
            phrase("Basic").thenReturn(Supertype.BASIC),
            phrase("Snow").thenReturn(Supertype.SNOW),
            phrase("World").thenReturn(Supertype.WORLD));

    public static final Parser<ZoneName> ZONE_NAME = anyOf(
            phrase("Battlefield").thenReturn(ZoneName.BATTLEFIELD),
            phrase("Graveyard").thenReturn(ZoneName.GRAVEYARD),
            phrase("Library").thenReturn(ZoneName.LIBRARY),
            phrase("Hand").thenReturn(ZoneName.HAND),
            phrase("Exile").thenReturn(ZoneName.EXILE),
            phrase("Stack").thenReturn(ZoneName.STACK),
            phrase("Command zone").thenReturn(ZoneName.COMMAND));

    /// Plural forms of zones that cards reference collectively
    /// ("all graveyards", "all libraries", "all hands").
    public static final Parser<ZoneName> PLURAL_ZONE_NAME = anyOf(
            phrase("Graveyards").thenReturn(ZoneName.GRAVEYARD),
            phrase("Libraries").thenReturn(ZoneName.LIBRARY),
            phrase("Hands").thenReturn(ZoneName.HAND));

    // ── Counter type ───────────────────────────────────────────────────

    private static final Parser<CounterType> PT_COUNTER =
            sequence(SIGNED_INT, string("/").then(SIGNED_INT), CounterType::ptCounter);

    /// Every counter type encountered in vintage-legal oracle text,
    /// plus rule-defined keyword and special-rule counters
    /// ({@mtg.rule 122.1b-i}). Each name is resolved through
    /// [CounterType#named] so keyword counters become
    /// [CounterType.Keyword] and the rest become
    /// [CounterType.Named]. Closed set — an unknown counter name is
    /// a parse failure, not a free-form fallback.
    private static final Parser<CounterType> NAMED_COUNTER = CounterType.BY_TEXT.entrySet().stream()
            .<Parser<CounterType>>map(e -> phrase(e.getKey()).thenReturn(e.getValue()))
            .collect(or());

    public static final Parser<CounterType> COUNTER_TYPE = anyOf(PT_COUNTER, NAMED_COUNTER);

    // ── P/T value ──────────────────────────────────────────────────────

    /// Atomic P/T value — a fixed integer or the variable "X".
    private static final Parser<Amount> PT_ATOM =
            anyOf(INTEGER.map(Amount::exact), word("X").thenReturn(Amount.variable()));

    public static final Parser<PtValue> PT_VALUE = sequence(PT_ATOM, string("/").then(PT_ATOM), PtValue::new);

    // ── Single type ────────────────────────────────────────────────────

    private static final Parser<Selector.SingleType> OBJECT_CARD_TYPE = sequence(
            GAME_OBJECT_TYPE.suchThat(t -> t != GameObjectType.SOURCE, "object type for compound"),
            CARD_TYPE,
            Selector.SingleType::objectCard);

    private static final Parser<Selector.SingleType> CARD_SINGLE = CARD_TYPE.map(Selector.SingleType::ofCard);

    private static final Parser<Selector.SingleType> OBJECT_SINGLE =
            GAME_OBJECT_TYPE.map(Selector.SingleType::ofGameObject);

    /// Builds a parser that accepts every spelling of the given subtype
    /// enum, returning the canonical enum constant. Each subtype's
    /// [text][Subtype#text()] is already a [Words#phrase]
    /// template (`"Goblin(s)"` / `"[Ally|Allies]"` /
    /// `"Eldrazi"`), so no further conversion is needed.
    private static <E extends Enum<E> & Subtype> Parser<Subtype> subtypeForms(E[] values) {
        return Arrays.stream(values)
                .<Parser<Subtype>>map(e -> phrase(e.text()).thenReturn(e))
                .collect(or());
    }

    public static final Parser<Subtype> SUBTYPE = anyOf(
            subtypeForms(CreatureType.values()),
            subtypeForms(LandType.values()),
            subtypeForms(ArtifactType.values()),
            subtypeForms(EnchantmentType.values()),
            subtypeForms(SpellType.values()),
            subtypeForms(BattleType.values()),
            subtypeForms(PlaneswalkerType.values()));

    private static final Parser<Selector.SingleType> SUBTYPE_SINGLE = SUBTYPE.map(Selector.SingleType::ofSubtype);

    /// "commander" / "commanders" — Commander-format role designation
    /// used in type-slot positions (Witch's Clinic: "target commander";
    /// Guardian Augmenter: "Commanders you control have hexproof.").
    /// Distinct from a subtype since commanders are not a MTG subtype
    /// (rule 205.3).
    private static final Parser<Selector.SingleType> ROLE_SINGLE =
            phrase("Commander(s)").thenReturn(Selector.SingleType.ofRole(Role.COMMANDER));

    static final Parser<Selector.SingleType> SINGLE_TYPE =
            anyOf(OBJECT_CARD_TYPE, CARD_SINGLE, OBJECT_SINGLE, SUBTYPE_SINGLE, ROLE_SINGLE);

    // ── Type expression ────────────────────────────────────────────────

    /// A refinement of a game object in an `"X or Y <object>"` construct:
    /// either a card type ("creature spell") or a subtype ("Aura spell").
    private sealed interface Refinement {
        record Card(CardType type) implements Refinement {}

        record SubtypeRef(Subtype subtype) implements Refinement {}
    }

    private static final Parser<Refinement> REFINEMENT =
            anyOf(CARD_TYPE.<Refinement>map(Refinement.Card::new), SUBTYPE.<Refinement>map(Refinement.SubtypeRef::new));

    private static Selector.SingleType combine(Refinement r, GameObjectType obj) {
        return switch (r) {
            case Refinement.Card c -> Selector.SingleType.objectCard(obj, c.type());
            case Refinement.SubtypeRef s -> Selector.SingleType.objectSubtype(obj, s.subtype());
        };
    }

    /// "[X] or [Y] [game-object]" — distributes the trailing game-object across
    /// each refinement. Examples: "creature or Aura spell", "Spirit or Arcane
    /// spell", "creature or sorcery spell". Must precede [#OR_TYPE] so
    /// the trailing game-object is not consumed by a `COMPOUND_TYPE`.
    private static final Parser<Selector.TypeExpression> OR_TYPE_WITH_OBJECT = sequence(
            REFINEMENT.followedBy(word("or")),
            REFINEMENT,
            GAME_OBJECT_TYPE,
            (a, b, obj) -> Selector.TypeExpression.orOfSingles(List.of(combine(a, obj), combine(b, obj))));

    /// "[X] and [Y] [game-object]" — same shape as [#OR_TYPE_WITH_OBJECT]
    /// with `and` instead of `or` (e.g., Arcane Melee: "Instant and sorcery
    /// spells cost {2} less to cast."). Modelled as an Or at the type level
    /// since both types qualify matching game-objects.
    private static final Parser<Selector.TypeExpression> AND_TYPE_WITH_OBJECT = sequence(
            REFINEMENT.followedBy(word("and")),
            REFINEMENT,
            GAME_OBJECT_TYPE,
            (a, b, obj) -> Selector.TypeExpression.orOfSingles(List.of(combine(a, obj), combine(b, obj))));

    /// A single type-group: one or more [#SINGLE_TYPE]s in
    /// sequence (e.g., "enchantment creature"). Emitted as a
    /// [Selector.TypeExpression.Single] for one type or a
    /// [Selector.TypeExpression.Compound] for two or more.
    private static final Parser<Selector.TypeExpression> TYPE_GROUP = SINGLE_TYPE
            .atLeastOnce()
            .map(list -> list.size() == 1
                    ? Selector.TypeExpression.single(list.getFirst())
                    : Selector.TypeExpression.compound(list));

    private static final Parser<Selector.TypeExpression> COMPOUND_TYPE = SINGLE_TYPE
            .atLeastOnce()
            .suchThat(list -> list.size() >= 2, "compound type")
            .map(Selector.TypeExpression::compound);

    private static final Parser<Selector.TypeExpression> SINGLE_WRAP = SINGLE_TYPE.map(Selector.TypeExpression::single);

    // OR_ALTERNATIVE / OR_TYPE / AND_TYPE / AND_OR_TYPE / TYPE_EXPRESSION
    // depend on QUALIFIER and are declared below the qualifier section.

    // ── Quantifier ─────────────────────────────────────────────────────

    public static final Parser<Selector.Quantifier> QUANTIFIER = anyOf(
            phrase("All").thenReturn(Selector.Quantifier.all()),
            // "both" — exactly two; modelled as a fixed Count(2)
            // (Alaborn Zealot: "destroy both creatures").
            phrase("Both").thenReturn(Selector.Quantifier.count(2)),
            phrase("Each").thenReturn(Selector.Quantifier.each()),
            phrase("Every").thenReturn(Selector.Quantifier.every()),
            phrase("Another").thenReturn(Selector.Quantifier.another()),
            phrase("Other").thenReturn(Selector.Quantifier.other()),
            phrase("The").thenReturn(Selector.Quantifier.the()),
            word("X").thenReturn(Selector.Quantifier.variable()),
            // "Up to N" / "up to X" — upper-bound quantifier
            // (Diabolic Revelation: "Search your library for up to X
            // cards, …"). The variable form uses -1 as a sentinel for
            // the bound-X max.
            phrase("Up to")
                    .then(anyOf(
                            anyOf(WORD_NUMBER, INTEGER).map(Selector.Quantifier::upTo),
                            word("X").thenReturn(Selector.Quantifier.upTo(-1)))),
            phrase("Any number of").thenReturn(Selector.Quantifier.anyNumber()),
            // "no \<type\>" — count-zero quantifier (Artificer's
            // Epiphany: "If you control no artifacts, …"; Tezzeret's
            // Ambition).
            phrase("No").thenReturn(Selector.Quantifier.none()),
            // "one or more" — at least one. Must precede the bare-integer
            // and "N or M" range arms so the literal prefix wins.
            phrase("One or more").thenReturn(Selector.Quantifier.range(1, Integer.MAX_VALUE)),
            // "at least N" — range starting at N, open-ended (Sokka:
            // "Whenever Sokka and at least one other creature
            // attack, …").
            phrase("At least")
                    .then(anyOf(WORD_NUMBER, INTEGER))
                    .map(n -> Selector.Quantifier.range(n, Integer.MAX_VALUE)),
            // "N or more" — at-least-N (Rampaging Ceratops: "except by
            // three or more creatures.").
            sequence(
                    anyOf(WORD_NUMBER, INTEGER),
                    phrase("or more"),
                    (n, _) -> Selector.Quantifier.range(n, Integer.MAX_VALUE)),
            // "N or fewer" / "N or less" — at-most-N (Fastlands cycle:
            // "This land enters tapped unless you control two or
            // fewer other lands."). Range from 0 to N inclusive.
            sequence(
                    anyOf(WORD_NUMBER, INTEGER),
                    anyOf(phrase("or fewer"), phrase("or less")),
                    (n, _) -> Selector.Quantifier.range(0, n)),
            // "N1, N2, or N3" — three-term Oxford range (Defend the
            // Celestus: "among one, two, or three target creatures
            // you control."). Must precede the two-term "N or M"
            // arm below.
            sequence(
                    anyOf(WORD_NUMBER, INTEGER).followedBy(","),
                    anyOf(WORD_NUMBER, INTEGER).followedBy(","),
                    word("or").then(anyOf(WORD_NUMBER, INTEGER)),
                    (a, b, c) -> Selector.Quantifier.range(Math.min(Math.min(a, b), c), Math.max(Math.max(a, b), c))),
            // "N or M" — inclusive range. Tried before bare N so the trailing
            // " or M" isn't left for a downstream selector-level "or".
            sequence(
                    anyOf(WORD_NUMBER, INTEGER),
                    word("or").then(anyOf(WORD_NUMBER, INTEGER)),
                    Selector.Quantifier::range),
            WORD_NUMBER.map(Selector.Quantifier::count),
            INTEGER.suchThat(n -> n > 1, "count > 1").map(Selector.Quantifier::count),
            phrase("[A|An]").thenReturn(Selector.Quantifier.one()),
            // "that many" — back-reference to an amount bound earlier
            // in the clause (Phyrexian Negator: "sacrifice that many
            // permanents.").
            phrase("That many").thenReturn(Selector.Quantifier.thatMany()),
            // "any <type>" — chooser-picks-one variant used in oracle
            // text like "a copy of any creature on the battlefield"
            // (Clone) or "the basic land type of your choice" idiom
            // counterparts. Semantically one of an unconstrained set,
            // so mapped to [Selector.Quantifier#one] like "a"/"an".
            // Kept last so the more specific "Any number of" wins.
            phrase("Any").thenReturn(Selector.Quantifier.one()));

    // ── Qualifier ──────────────────────────────────────────────────────

    private static final Parser<Selector.Qualifier> TARGET_Q = phrase("Target").thenReturn(Selector.Qualifier.TARGET);

    /// Color-atom parser — re-exposed from [ColorQualifierParsers] for
    /// the static-import sites in this file that consume single colors
    /// outside a qualifier context.
    static final Parser<ColorMatcher> COLOR_FILTER = ColorQualifierParsers.COLOR_FILTER;

    private static final Parser<Selector.Qualifier> COLOR_Q = ColorQualifierParsers.COLOR_Q;

    /// Supertype qualifier — re-exposed from [TypeQualifierParsers].
    /// Produces a [Selector.Qualifier.Supertypes] wrapping a
    /// [SupertypeMatcher] boolean tree (typically `Is(...)` for
    /// positive forms; `Not(...)` for negated; `All` after the
    /// list-merge fold).
    private static final Parser<Selector.Qualifier> SUPERTYPE_Q = TypeQualifierParsers.SUPERTYPE_Q;

    private static final Parser<Selector.Qualifier> NEGATED_SUPERTYPE_Q = TypeQualifierParsers.NEGATED_SUPERTYPE_Q;

    static final Parser<Selector.Qualifier> NEGATED_CARD_TYPE_Q = TypeQualifierParsers.NEGATED_CARD_TYPE_Q;

    private static final Parser<Selector.Qualifier> NEGATED_SUBTYPE_Q = TypeQualifierParsers.NEGATED_SUBTYPE_Q;

    private static final Parser<Selector.Qualifier> STATUS_Q = anyOf(
            phrase("Tapped").thenReturn(Selector.Qualifier.Status.TAPPED),
            phrase("Untapped").thenReturn(Selector.Qualifier.Status.UNTAPPED),
            phrase("Face-down").thenReturn(Selector.Qualifier.Status.FACE_DOWN),
            phrase("Face-up").thenReturn(Selector.Qualifier.Status.FACE_UP),
            phrase("Phased-out").thenReturn(Selector.Qualifier.Status.PHASED_OUT),
            // "exiled" — zone-located in exile, used as an adjectival
            // qualifier (Pull from Eternity: "target face-up exiled
            // card").
            phrase("Exiled").thenReturn(Selector.Qualifier.Status.EXILED),
            // Resolution-history participles — cards that were acted on
            // during the current resolution (Heed the Mists: "the milled
            // card's mana value").
            phrase("Milled").thenReturn(Selector.Qualifier.Status.MILLED),
            phrase("Drawn").thenReturn(Selector.Qualifier.Status.DRAWN),
            phrase("Discarded").thenReturn(Selector.Qualifier.Status.DISCARDED),
            phrase("Revealed").thenReturn(Selector.Qualifier.Status.REVEALED),
            // "transformed" — showing its back face (Mutagen Connoisseur).
            phrase("Transformed").thenReturn(Selector.Qualifier.Status.TRANSFORMED),
            // "suspended" — Venser's Diffusion: "Return target nonland
            // permanent or suspended card to its owner's hand.".
            phrase("Suspended").thenReturn(Selector.Qualifier.Status.SUSPENDED),
            // "noncommander" — Commander-format negation (Subjugate
            // the Hobbits: "each noncommander creature"). The
            // positive form "commander" is handled through the
            // SUBTYPE parser (CreatureType.COMMANDER) so "target
            // commander" parses as a bare type rather than an
            // orphan qualifier.
            phrase("Noncommander").thenReturn(Selector.Qualifier.Status.NONCOMMANDER));

    private static final Parser<Selector.Qualifier> COMBAT_STATUS_Q = anyOf(
            // Multi-word combined forms first (longer match before shorter).
            phrase("attacking or blocking").thenReturn(Selector.Qualifier.combatStatus("attacking or blocking")),
            // "attacking you" — directed attack marker (e.g., Blessed Reversal:
            // "for each creature attacking you").
            phrase("attacking you").thenReturn(Selector.Qualifier.combatStatus("attacking you")),
            phrase("Attacking").thenReturn(Selector.Qualifier.combatStatus("attacking")),
            phrase("Blocking").thenReturn(Selector.Qualifier.combatStatus("blocking")),
            phrase("Blocked").thenReturn(Selector.Qualifier.combatStatus("blocked")),
            phrase("Unblocked").thenReturn(Selector.Qualifier.combatStatus("unblocked")),
            // Negated combat statuses (Alarum: "target nonattacking creature").
            phrase("Nonattacking").thenReturn(Selector.Qualifier.combatStatus("nonattacking")),
            phrase("Nonblocking").thenReturn(Selector.Qualifier.combatStatus("nonblocking")));

    private static final Parser<Selector.Qualifier> HISTORIC_Q =
            phrase("Historic").thenReturn(Selector.Qualifier.HISTORIC);

    /// "activated" / "triggered" — ability-source qualifier on an
    /// ability target (Tale's End: "target activated ability, triggered
    /// ability, or legendary spell"). Distinguishes 113.3a activated
    /// from 113.3b triggered abilities when the selector targets an
    /// ability on the stack.
    private static final Parser<Selector.Qualifier> ABILITY_SOURCE_Q = anyOf(
            word("activated").thenReturn(Selector.Qualifier.AbilitySource.ACTIVATED),
            word("triggered").thenReturn(Selector.Qualifier.AbilitySource.TRIGGERED));

    /// "last" / "first" / "top" — positional qualifier (Jandor's Ring:
    /// "the last card you drew this turn"). Rendered as a status-style
    /// qualifier since these aren't formal supertypes.
    private static final Parser<Selector.Qualifier> POSITIONAL_Q = anyOf(
            phrase("Last").thenReturn(Selector.Qualifier.Status.LAST),
            phrase("First").thenReturn(Selector.Qualifier.Status.FIRST),
            phrase("Top").thenReturn(Selector.Qualifier.Status.TOP));

    private static final Parser<Selector.Qualifier> OUTLAW_Q = phrase("Outlaw").thenReturn(Selector.Qualifier.OUTLAW);

    private static final Parser<Selector.Qualifier> NON_OUTLAW_Q =
            string("non-").then(word("outlaw")).thenReturn(Selector.Qualifier.NEGATED_OUTLAW);

    private static final Parser<Selector.Qualifier> NONTOKEN_Q =
            phrase("Nontoken").thenReturn(Selector.Qualifier.NON_TOKEN);

    private static final Parser<Selector.Qualifier> OTHER_Q = phrase("Other").thenReturn(Selector.Qualifier.OTHER);

    private static final Parser<Selector.Qualifier> ENCHANTED_Q =
            phrase("Enchanted").thenReturn(Selector.Qualifier.Enchanted.ENCHANTED);

    /// "kicked" — spell cast with its kicker cost (rule 702.33). Used
    /// as a selector qualifier on spell triggers (Merfolk Falconer).
    private static final Parser<Selector.Qualifier> KICKED_Q =
            phrase("Kicked").thenReturn(Selector.Qualifier.combatStatus("kicked"));

    private static final Parser<Selector.Qualifier> EQUIPPED_Q =
            phrase("Equipped").thenReturn(Selector.Qualifier.Equipped.EQUIPPED);

    /// "X/Y" — a P/T as a selector qualifier (Aegis of the Meek:
    /// "Target 1/1 creature").
    private static final Parser<Selector.Qualifier> PT_QUALIFIER_Q = PT_VALUE.map(Selector.Qualifier.PtQualifier::new);

    static final Parser<Selector.Qualifier> QUALIFIER = anyOf(
            TARGET_Q,
            COLOR_Q,
            NEGATED_SUPERTYPE_Q,
            NEGATED_CARD_TYPE_Q,
            NON_OUTLAW_Q,
            NEGATED_SUBTYPE_Q,
            SUPERTYPE_Q,
            STATUS_Q,
            COMBAT_STATUS_Q,
            // Reject ABILITY_SOURCE_Q when followed by "or <another
            // ABILITY_SOURCE_Q>" so the shared-noun distributive form
            // ("activated or triggered ability" — Stifle) is left for
            // QUALIFIER_OR_WITH_OBJECT to consume as a whole.
            ABILITY_SOURCE_Q.notFollowedBy(word("or").then(ABILITY_SOURCE_Q), "distributive qualifier-or"),
            HISTORIC_Q,
            POSITIONAL_Q,
            OUTLAW_Q,
            NONTOKEN_Q,
            OTHER_Q,
            ENCHANTED_Q,
            EQUIPPED_Q,
            KICKED_Q,
            PT_QUALIFIER_Q);

    /// One-or-more qualifiers, optionally interleaved with commas so
    /// "non-X, non-Y, non-Z creature" (Victim of Night: "non-Vampire,
    /// non-Werewolf, non-Zombie creature") flattens into a single qualifier
    /// list. Each qualifier may absorb a trailing comma as glue — the result
    /// is a flat `List<Qualifier>`, not a structured conjunction.
    private static final Parser<List<Selector.Qualifier>> QUALIFIER_LIST = QUALIFIER
            .optionallyFollowedBy(",")
            .atLeastOnce()
            .map(ColorQualifierParsers::mergeColorQualifiers)
            .map(TypeQualifierParsers::mergeTypeQualifiers);

    // ── Or-alternative and TYPE_EXPRESSION (depend on QUALIFIER) ──────

    // OR_ALTERNATIVE / OR_TYPE / AND_TYPE / AND_OR_TYPE / TYPE_EXPRESSION
    // are declared below the WITH_CLAUSE section because an alternative
    // may carry a trailing `with [clause]` suffix.

    // ── With clause ────────────────────────────────────────────────────

    /// Words that signal the end of the selector and the start of an outer
    /// effect clause — used to bound the WITH_CLAUSE predicate so it doesn't
    /// greedily consume "get" / "gets" / "can't" / etc. after a "with" clause.
    private static final Set<String> WITH_STOP_WORDS = Set.of(
            "get",
            "gets",
            "have",
            "has",
            "deal",
            "deals",
            "enter",
            "enters",
            "are",
            "is",
            "ca",
            "can",
            "can't",
            "lose",
            "loses",
            "gain",
            "gains",
            "attack",
            "attacks",
            "block",
            "blocks",
            // "die"/"dies" and "leave"/"leaves" are trigger-event verbs
            // that mark the start of the trigger body after the subject
            // (Meltstrider Eulogist: "Whenever a creature … with a +1/+1
            // counter on it dies, draw a card.").
            "die",
            "dies",
            "leave",
            "leaves",
            // "phase"/"phases" — Time and Tide: "all creatures with
            // phasing phase out". Without this stop word the WITH_CLAUSE
            // free-text predicate would consume "phasing phase out".
            "phase",
            "phases",
            // "cost" / "costs" bound the with-predicate so
            // MODIFY_COST's verb ("cost \[mana\] more/less") stays
            // available (Krosan Drover: "Creature spells you cast
            // with mana value 6 or greater cost {2} less to cast.").
            "cost",
            "costs",
            // "paying" bounds the `without` in "without paying [its|their]
            // mana cost" so CAST_WITHOUT_PAYING can match it at the outer
            // effect level (Dracogenesis).
            "paying",
            // "able" bounds "with flying" so "able to block …" stays
            // available for ABLE_TO_BLOCK_DO_SO (Talruum Piper).
            "able",
            // "to" bounds "with flashback" so BOUNCE destinations ("to your
            // hand" — Runic Repetition) aren't swallowed into the with-clause
            // predicate.
            "to",
            // "as" / "though" bound "with enchant creature" so the outer
            // CAST_AS_THOUGH ("as though they had flash" — Rootwater Shaman)
            // can consume the tail instead of swallowing it into the
            // with-predicate.
            "as",
            "though",
            // "you" / "they" / "an" bound "with flashback" so a trailing
            // controller-clause ("you own", "you control", "they own", "an
            // opponent controls") remains available for the outer Selector
            // (Runic Repetition: "target exiled card with flashback you
            // own").
            "you",
            "they",
            "an",
            // "unless" bounds the with-predicate so trailing "unless
            // <predicate>" conditions on the outer effect remain
            // reachable (Hipparion: "can't block creatures with power
            // 3 or greater unless you pay {1}.").
            "unless");

    /// Keyword abilities that may appear in a "with <keyword>" clause
    /// (e.g., "with flying", "with first strike"). Maps each canonical
    /// oracle-text name to its [Ability] constant. We don't reuse
    /// [KeywordParsers#SIMPLE] here because it transitively
    /// references [SubjectParsers], which loops back through
    /// [SelectorParsers] and triggers a static-init NPE.
    private static final Parser<Ability> WITH_KEYWORD_NAME = anyOf(
            // Multi-word first so the leading word isn't consumed alone.
            phrase("double strike").thenReturn(Ability.StaticKeyword.DOUBLE_STRIKE),
            phrase("first strike").thenReturn(Ability.StaticKeyword.FIRST_STRIKE),
            word("deathtouch").thenReturn(Ability.StaticKeyword.DEATHTOUCH),
            word("defender").thenReturn(Ability.StaticKeyword.DEFENDER),
            word("flash").thenReturn(Ability.StaticKeyword.FLASH),
            word("flying").thenReturn(Ability.StaticKeyword.FLYING),
            word("haste").thenReturn(Ability.StaticKeyword.HASTE),
            word("hexproof").thenReturn(Ability.StaticKeyword.HEXPROOF),
            word("indestructible").thenReturn(Ability.StaticKeyword.INDESTRUCTIBLE),
            word("intimidate").thenReturn(Ability.StaticKeyword.INTIMIDATE),
            word("lifelink").thenReturn(Ability.StaticKeyword.LIFELINK),
            word("menace").thenReturn(Ability.StaticKeyword.MENACE),
            word("reach").thenReturn(Ability.StaticKeyword.REACH),
            word("shroud").thenReturn(Ability.StaticKeyword.SHROUD),
            word("trample").thenReturn(Ability.StaticKeyword.TRAMPLE),
            word("vigilance").thenReturn(Ability.StaticKeyword.VIGILANCE),
            word("banding").thenReturn(Ability.StaticKeyword.BANDING),
            word("fear").thenReturn(Ability.StaticKeyword.FEAR),
            word("flanking").thenReturn(Ability.TriggeredKeyword.FLANKING),
            word("horsemanship").thenReturn(Ability.StaticKeyword.HORSEMANSHIP),
            word("shadow").thenReturn(Ability.StaticKeyword.SHADOW),
            word("infect").thenReturn(Ability.StaticKeyword.INFECT),
            word("wither").thenReturn(Ability.StaticKeyword.WITHER),
            word("skulk").thenReturn(Ability.StaticKeyword.SKULK),
            word("devoid").thenReturn(Ability.StaticKeyword.DEVOID),
            word("phasing").thenReturn(Ability.StaticKeyword.PHASING));

    /// Token inside a free-text with-clause predicate — plain words plus
    /// "+1/+1" / "-1/-1" counter markers (Herald of Secret Streams) and
    /// possessive apostrophes (Wandering Wolf: "Creatures with power
    /// less than this creature's power").
    private static final Parser<String> WITH_PREDICATE_TOKEN = anyOf(
            consecutive(CharacterSet.charsIn("[0-9+/-]"), "with-predicate pt marker"),
            consecutive(CharacterSet.charsIn("[A-Za-z'-]"), "with-predicate word"));

    static final Parser<Selector.WithClause> WITH_CLAUSE = sequence(
            anyOf(phrase("with").thenReturn(false), phrase("without").thenReturn(true)),
            // Try an or-list of keyword abilities first ("with flying or reach" —
            // Orchard Spirit) so the trailing ability isn't consumed as a
            // free-text predicate. Then try a single structural keyword-ability
            // reference ("with flying" becomes {@link WithClause.HasAbility}).
            // Fall back to a free-text predicate for phrases the grammar hasn't
            // structured yet (e.g., "with flashback", "with cycling", "with a
            // +1/+1 counter on it").
            Parser.<Selector.WithClause>anyOf(
                    MtgParsers.orList(WITH_KEYWORD_NAME)
                            .suchThat(l -> l.size() >= 2, "or-list of with-abilities")
                            .map(abilities ->
                                    (Selector.WithClause) new Selector.WithClause.HasAnyAbility(false, abilities)),
                    WITH_KEYWORD_NAME.map(
                            ability -> (Selector.WithClause) new Selector.WithClause.HasAbility(false, ability)),
                    // "mana value of the chosen quality" — back-
                    // reference to a preceding [Effect.ChooseQuality]
                    // (Extinction Event). Must precede the free-text
                    // branch so "of the chosen quality" doesn't get
                    // eaten as predicate words.
                    phrase("mana value of the chosen quality")
                            .<Selector.WithClause>thenReturn(
                                    new Selector.WithClause.HasManaValueOfChosenQuality(false)),
                    // "the chosen name" — back-reference to a
                    // preceding [Effect.ChooseCardName] (Declaration
                    // of Naught).
                    phrase("the chosen name")
                            .<Selector.WithClause>thenReturn(new Selector.WithClause.HasChosenName(false)),
                    // "the same name as \[demonstrative\]" — name-equality
                    // (Wake of Destruction). Must precede the free-text
                    // branch so the "as" stop-word doesn't terminate the
                    // predicate prematurely.
                    sequence(
                                    phrase("the same name as"),
                                    anyOf(word("that"), word("this"), word("those")),
                                    anyOf(
                                            word("land"),
                                            word("creature"),
                                            word("permanent"),
                                            word("card"),
                                            word("artifact"),
                                            word("enchantment"),
                                            word("spell")),
                                    (_, det, type) -> det + " " + type)
                            .map(ref -> (Selector.WithClause) new Selector.WithClause.SameNameAs(false, ref)),
                    // "power|toughness N or greater|less|more" —
                    // postfix structural comparison (Eternal
                    // Isolation: "target creature with power 4 or
                    // greater"). Must precede both the "cmp reference"
                    // form (so "4" isn't read as a reference to
                    // "greater") and the free-text branch (so "on"
                    // doesn't get eaten as predicate words).
                    sequence(
                            anyOf(
                                    word("power").thenReturn(Selector.WithClause.PtComparison.Aspect.POWER),
                                    word("toughness").thenReturn(Selector.WithClause.PtComparison.Aspect.TOUGHNESS)),
                            consecutive(CharacterSet.charsIn("[0-9]"), "integer"),
                            anyOf(
                                    phrase("or greater")
                                            .thenReturn(
                                                    Selector.WithClause.PtComparison.Comparator.GREATER_THAN_OR_EQUAL),
                                    phrase("or more")
                                            .thenReturn(
                                                    Selector.WithClause.PtComparison.Comparator.GREATER_THAN_OR_EQUAL),
                                    phrase("or less")
                                            .thenReturn(
                                                    Selector.WithClause.PtComparison.Comparator.LESS_THAN_OR_EQUAL)),
                            (aspect, n, cmp) ->
                                    (Selector.WithClause) new Selector.WithClause.PtComparison(false, aspect, cmp, n)),
                    // "power|toughness \[cmp\] \[reference\]" — structural
                    // comparison (Blazing Hope: "with power greater
                    // than or equal to your life total"). Must precede
                    // the free-text branch so the trailing "to …"
                    // isn't clipped by the "to" stop-word.
                    sequence(
                            anyOf(
                                    word("power").thenReturn(Selector.WithClause.PtComparison.Aspect.POWER),
                                    word("toughness").thenReturn(Selector.WithClause.PtComparison.Aspect.TOUGHNESS)),
                            anyOf(
                                    phrase("greater than or equal to")
                                            .thenReturn(
                                                    Selector.WithClause.PtComparison.Comparator.GREATER_THAN_OR_EQUAL),
                                    phrase("less than or equal to")
                                            .thenReturn(Selector.WithClause.PtComparison.Comparator.LESS_THAN_OR_EQUAL),
                                    phrase("greater than")
                                            .thenReturn(Selector.WithClause.PtComparison.Comparator.GREATER_THAN),
                                    phrase("less than")
                                            .thenReturn(Selector.WithClause.PtComparison.Comparator.LESS_THAN),
                                    phrase("equal to").thenReturn(Selector.WithClause.PtComparison.Comparator.EQUAL)),
                            WITH_PREDICATE_TOKEN
                                    .suchThat(w -> !WITH_STOP_WORDS.contains(w.toLowerCase()), "with-clause word")
                                    .atLeastOnce()
                                    .map(ws -> String.join(" ", ws)),
                            (aspect, cmp, ref) -> (Selector.WithClause)
                                    new Selector.WithClause.PtComparison(false, aspect, cmp, ref)),
                    WITH_PREDICATE_TOKEN
                            .suchThat(w -> !WITH_STOP_WORDS.contains(w.toLowerCase()), "with-clause word")
                            .atLeastOnce()
                            .map(words -> (Selector.WithClause)
                                    new Selector.WithClause.HasPredicate(false, String.join(" ", words)))),
            (negated, clause) -> switch (clause) {
                case Selector.WithClause.HasAbility ha -> new Selector.WithClause.HasAbility(negated, ha.ability());
                case Selector.WithClause.HasAnyAbility haa ->
                    new Selector.WithClause.HasAnyAbility(negated, haa.abilities());
                case Selector.WithClause.HasPredicate hp ->
                    new Selector.WithClause.HasPredicate(negated, hp.predicate());
                case Selector.WithClause.SameNameAs sn -> new Selector.WithClause.SameNameAs(negated, sn.reference());
                case Selector.WithClause.HasName hn -> new Selector.WithClause.HasName(negated, hn.name());
                case Selector.WithClause.HasManaValueOfChosenQuality hmv ->
                    new Selector.WithClause.HasManaValueOfChosenQuality(negated);
                case Selector.WithClause.HasChosenName hcn -> new Selector.WithClause.HasChosenName(negated);
                case Selector.WithClause.PtComparison pc ->
                    new Selector.WithClause.PtComparison(negated, pc.aspect(), pc.cmp(), pc.reference());
            });

    // ── Or-alternative and TYPE_EXPRESSION (depend on QUALIFIER and WITH_CLAUSE) ─

    /// One disjunct in an [Selector.TypeExpression.Or] — optional
    /// leading qualifiers, a [#TYPE_GROUP], and an optional
    /// trailing [#WITH_CLAUSE]. Each alternative owns its own
    /// qualifiers and with-clauses so "enchanted creature or enchantment
    /// creature" and "Spirit, creature with disturb, or enchantment"
    /// round-trip with branch-local context.
    /// A player-role alternative inside an Oxford-comma type or-list
    /// — maps "opponent" / "player" to an [Or.Alternative] whose
    /// type is the PLAYER game-object and whose qualifier names the
    /// specific role (Price of Betrayal: "target artifact, creature,
    /// planeswalker, or opponent").
    private static final Parser<Selector.TypeExpression.Or.Alternative> PLAYER_ROLE_ALTERNATIVE = anyOf(
                    word("opponent").thenReturn(Subject.PlayerRef.AN_OPPONENT),
                    word("player").thenReturn(Subject.PlayerRef.A_PLAYER))
            .map(role -> new Selector.TypeExpression.Or.Alternative(
                    List.of(new Selector.Qualifier.PlayerRole(role)),
                    Selector.TypeExpression.single(Selector.SingleType.ofGameObject(GameObjectType.PLAYER))));

    private static final Parser<Selector.TypeExpression.Or.Alternative> OR_ALTERNATIVE = anyOf(
                    sequence(QUALIFIER_LIST, TYPE_GROUP, Selector.TypeExpression.Or.Alternative::new),
                    TYPE_GROUP.map(Selector.TypeExpression.Or.Alternative::new),
                    PLAYER_ROLE_ALTERNATIVE)
            .optionallyFollowedBy(WITH_CLAUSE, (alt, wc) -> alt.withWithClauses(List.of(wc)));

    /// "X or Y" / "X, Y, or Z" / "W, X, Y, or Z" — Oxford-comma or-list of
    /// alternatives (Reclaiming Vines: "artifact, enchantment, or land";
    /// Feast of Dreams: "enchanted creature or enchantment creature").
    private static final Parser<Selector.TypeExpression> OR_TYPE = MtgParsers.orList(OR_ALTERNATIVE)
            .suchThat(l -> l.size() >= 2, "or-list of alternatives")
            .map(Selector.TypeExpression::or);

    /// "X and/or Y" — either or both of two types qualify (Mass
    /// Manipulation: "X target creatures and/or planeswalkers.").
    private static final Parser<Selector.TypeExpression> AND_OR_TYPE = MtgParsers.andOrList(OR_ALTERNATIVE)
            .suchThat(l -> l.size() >= 2, "and/or-list of alternatives")
            .map(Selector.TypeExpression::or);

    /// "X and Y" / "X, Y, and Z" — union of types. Modelled as an Or
    /// since any listed type matches.
    private static final Parser<Selector.TypeExpression> AND_TYPE = MtgParsers.andList(OR_ALTERNATIVE)
            .suchThat(l -> l.size() >= 2, "and-list of alternatives")
            .map(Selector.TypeExpression::or);

    /// "\[q1\] or \[q2\] \[game-object\]" — two qualifiers sharing one
    /// trailing game-object noun (Stifle: "activated or triggered
    /// ability"). Distributes the noun across each qualifier,
    /// producing an Or with one Alternative per qualifier. Restricted
    /// to [#ABILITY_SOURCE_Q] for now since that's where the shared-
    /// noun shorthand shows up in oracle text.
    private static final Parser<Selector.TypeExpression> QUALIFIER_OR_WITH_OBJECT = sequence(
            ABILITY_SOURCE_Q.followedBy(word("or")),
            ABILITY_SOURCE_Q,
            GAME_OBJECT_TYPE,
            (q1, q2, obj) -> Selector.TypeExpression.or(List.of(
                    new Selector.TypeExpression.Or.Alternative(
                            List.of(q1), Selector.TypeExpression.single(Selector.SingleType.ofGameObject(obj))),
                    new Selector.TypeExpression.Or.Alternative(
                            List.of(q2), Selector.TypeExpression.single(Selector.SingleType.ofGameObject(obj))))));

    public static final Parser<Selector.TypeExpression> TYPE_EXPRESSION = anyOf(
            OR_TYPE_WITH_OBJECT,
            AND_TYPE_WITH_OBJECT,
            QUALIFIER_OR_WITH_OBJECT,
            // AND_OR_TYPE must precede OR_TYPE/AND_TYPE — the "and/or"
            // literal would otherwise be half-consumed as "and" or "or".
            AND_OR_TYPE,
            OR_TYPE,
            AND_TYPE,
            COMPOUND_TYPE,
            SINGLE_WRAP);

    /// A word-with-contraction token (e.g., "isn't", "doesn't"). Broader than
    /// [Parser#word()] so relative clauses can include English
    /// contractions like "that isn't all colors".
    private static final Parser<String> CONTRACTION_WORD = consecutive(CharacterSet.charsIn("[A-Za-z0-9'-]"), "word");

    /// Stop-words for the that-clause predicate — narrower than
    /// [#WITH_STOP_WORDS] because `that are …` / `that is …` are
    /// valid opening forms (e.g., Song of Serenity: "creatures that are
    /// enchanted …"), so "are"/"is" must be allowed inside the predicate.
    /// Contractions like `can't` are captured as a single
    /// [#CONTRACTION_WORD] token and need explicit entries here.
    private static final Set<String> THAT_STOP_WORDS = Set.of(
            "get", "gets", "have", "has", "deal", "deals", "enter", "enters", "can", "can't", "lose", "loses", "gain",
            "gains", "attack", "attacks", "block", "blocks", "must", "cost", "costs");

    /// Lookahead used by [#THAT_CLAUSE] to recognize a trailing
    /// "to <destination>" tail (e.g., "to its owner's hand", "to the
    /// battlefield"). When "to" is followed by one of these shapes the
    /// that-clause stops so the surrounding bounce/move parser can
    /// consume the destination instead. "to <player>" (Reciprocate:
    /// "dealt damage to you this turn") doesn't match and is kept inside
    /// the predicate.
    private static final Parser<?> DESTINATION_AFTER_TO = anyOf(
            phrase("the battlefield"),
            phrase("their owners' hands"),
            phrase("its owner's hand"),
            phrase("their owner's hand"),
            phrase("your hand"),
            phrase("their hand"));

    /// One token of a [#THAT_CLAUSE] predicate. Either:
    /// - "to" when it isn't introducing a destination (stays in the
    ///   predicate; e.g., Reciprocate).
    /// - any other contraction-word that isn't a verb stop word.
    /// Order matters: the "to" arm is tried first so the lookahead can
    /// short-circuit before the catch-all matches "to" via the second
    /// arm.
    private static final Parser<String> THAT_CLAUSE_WORD = anyOf(
            phrase("to").notFollowedBy(DESTINATION_AFTER_TO, "to-destination"),
            // Allow mana symbols inside that-clause predicates so
            // "activated ability with {T} in its cost" (Magewright's
            // Stone) round-trips verbatim.
            consecutive(CharacterSet.charsIn("[{}A-Za-z0-9]"), "mana symbol")
                    .suchThat(s -> s.startsWith("{") && s.endsWith("}"), "mana-symbol token"),
            CONTRACTION_WORD.suchThat(
                    w -> !THAT_STOP_WORDS.contains(w.toLowerCase()) && !w.equalsIgnoreCase("to"), "that-clause word"));

    /// Token parser inside a "that has …" predicate. Allows the shared
    /// "has" / "cost" stop words — once the outer clause has committed
    /// to the "that has" prefix, those words belong to the predicate
    /// rather than to the enclosing effect's verb.
    private static final Parser<String> THAT_HAS_WORD = anyOf(
            phrase("to").notFollowedBy(DESTINATION_AFTER_TO, "to-destination"),
            consecutive(CharacterSet.charsIn("[{}A-Za-z0-9]"), "mana symbol")
                    .suchThat(s -> s.startsWith("{") && s.endsWith("}"), "mana-symbol token"),
            CONTRACTION_WORD.suchThat(w -> !w.equalsIgnoreCase("to"), "that-has word"));

    /// "that [predicate]" — relative clause. Stops at the containing
    /// effect's verb (see [#THAT_STOP_WORDS]) or before a
    /// "to <destination>" tail (see [#DESTINATION_AFTER_TO]).
    /// The "that has \[predicate\]" form takes a longer prefix so the
    /// shared "has" / "cost" stop-words don't terminate the clause
    /// prematurely (Magewright's Stone: "target creature that has an
    /// activated ability with {T} in its cost.").
    /// One element of a "that's \[a|an\]? X, \[a|an\]? Y, or \[a|an\]? Z"
    /// subtype-disjunction list (Lovisa Coldeyes: "each creature that's
    /// a Barbarian, a Warrior, or a Berserker"; Sporecrown Thallid:
    /// "each other creature you control that's a Fungus or Saproling" —
    /// tail article dropped).
    private static final Parser<Subtype> SUBTYPE_A_N = anyOf(phrase("[a|an]").then(SUBTYPE), SUBTYPE);

    /// Package-visible so [SubjectParsers#ANY_TARGET] can attach a
    /// trailing that-clause to "any target" (Needle Drop).
    static final Parser<Selector.ThatClause> THAT_CLAUSE = anyOf(
            // "that's \[a|an\] X[, \[a|an\] Y]*[, or \[a|an\] Z]" —
            // subtype disjunction as a relative clause (Lovisa
            // Coldeyes). Captured verbatim as a predicate; the
            // string encoding preserves the list.
            phrase("that's")
                    .then(MtgParsers.orList(SUBTYPE_A_N))
                    .suchThat(l -> l.size() >= 1, "at least one subtype")
                    .map(subs -> new Selector.ThatClause.Predicate("is "
                            + subs.stream()
                                    .map(st -> "a " + st.texts().getFirst())
                                    .collect(Collectors.joining(", ")))),
            phrase("that has")
                    .then(THAT_HAS_WORD.atLeastOnce().map(words -> "has " + String.join(" ", words)))
                    .map(Selector.ThatClause.Predicate::new),
            phrase("that")
                    .then(THAT_CLAUSE_WORD.atLeastOnce().map(words -> String.join(" ", words)))
                    .map(Selector.ThatClause.Predicate::new));

    // ── Controller clause ──────────────────────────────────────────────

    private static Selector.ControllerClause controls(Selector.ControllerClause.Who who, boolean negated) {
        return new Selector.ControllerClause.Controls(who, negated);
    }

    private static final Parser<Selector.ControllerClause> CONTROLLER_CLAUSE = anyOf(
            phrase("you don't control").thenReturn(controls(Selector.ControllerClause.Who.YOU, true)),
            // "you both own and control" — combined ownership+controller
            // predicate (Obelisk of Undoing: "target permanent you both own
            // and control"). Structured as an [OwnsAndControls] clause so
            // consumers can distinguish it from plain control.
            phrase("you both own and control").thenReturn((Selector.ControllerClause)
                    new Selector.ControllerClause.OwnsAndControls(Selector.ControllerClause.Who.YOU)),
            phrase("you control").thenReturn(controls(Selector.ControllerClause.Who.YOU, false)),
            phrase("you cast")
                    .<Selector.ControllerClause>thenReturn(
                            new Selector.ControllerClause.Casts(Selector.ControllerClause.Who.YOU))
                    // "this turn" — temporal scope (Goblin Maskmaker:
                    // "face-down spells you cast this turn cost {1} less
                    // to cast."). Absorbed as flavor since the structural
                    // clause already binds the controller; downstream
                    // consumers infer the turn boundary from context.
                    .optionallyFollowedBy(phrase("this turn"), (c, _) -> c),
            // "you've cast" — past-tense contraction (Multani's Presence:
            // "a spell you've cast"). Single phrase so it can't
            // partially commit mid-match.
            phrase("you've cast").thenReturn((Selector.ControllerClause)
                    new Selector.ControllerClause.Casts(Selector.ControllerClause.Who.YOU)),
            // "you've discarded \[this turn\]?" — past-tense discard
            // history (Change of Fortune: "draw a card for each card
            // you've discarded this turn."). The optional "this turn"
            // temporal scope is absorbed as flavor since the structural
            // [Discarded] clause already implies it in current usage.
            phrase("you've discarded")
                    .<Selector.ControllerClause>thenReturn(
                            new Selector.ControllerClause.Discarded(Selector.ControllerClause.Who.YOU))
                    .optionallyFollowedBy(phrase("this turn"), (c, _) -> c),
            // "you're attacking" — present-progressive attacker scope
            // (Astral Confrontation: "for each opponent you're
            // attacking."). The clause selects defenders the controller
            // currently has attackers declared against.
            phrase("you're attacking").thenReturn((Selector.ControllerClause)
                    new Selector.ControllerClause.Attacking(Selector.ControllerClause.Who.YOU)),
            phrase("your team controls").thenReturn(controls(Selector.ControllerClause.Who.YOUR_TEAM, false)),
            phrase("an opponent controls").thenReturn(controls(Selector.ControllerClause.Who.AN_OPPONENT, false)),
            phrase("each opponent controls").thenReturn(controls(Selector.ControllerClause.Who.EACH_OPPONENT, false)),
            phrase("your opponents control").thenReturn(controls(Selector.ControllerClause.Who.YOUR_OPPONENTS, false)),
            phrase("your opponents cast").thenReturn((Selector.ControllerClause)
                    new Selector.ControllerClause.Casts(Selector.ControllerClause.Who.YOUR_OPPONENTS)),
            phrase("an opponent casts").thenReturn((Selector.ControllerClause)
                    new Selector.ControllerClause.Casts(Selector.ControllerClause.Who.AN_OPPONENT)),
            phrase("target player controls").thenReturn(controls(Selector.ControllerClause.Who.TARGET_PLAYER, false)),
            phrase("target opponent controls")
                    .thenReturn(controls(Selector.ControllerClause.Who.TARGET_OPPONENT, false)),
            phrase("defending player controls")
                    .thenReturn(controls(Selector.ControllerClause.Who.DEFENDING_PLAYER, false)),
            phrase("enchanted player controls")
                    .thenReturn(controls(Selector.ControllerClause.Who.ENCHANTED_PLAYER, false)),
            phrase("its controller controls").thenReturn(controls(Selector.ControllerClause.Who.ITS_CONTROLLER, false)),
            phrase("they control").thenReturn(controls(Selector.ControllerClause.Who.THEY, false)),
            phrase("target player owns").thenReturn((Selector.ControllerClause)
                    new Selector.ControllerClause.Owns(Selector.ControllerClause.Who.TARGET_PLAYER)),
            phrase("you own").thenReturn((Selector.ControllerClause)
                    new Selector.ControllerClause.Owns(Selector.ControllerClause.Who.YOU)),
            phrase("an opponent owns").thenReturn((Selector.ControllerClause)
                    new Selector.ControllerClause.Owns(Selector.ControllerClause.Who.AN_OPPONENT)),
            phrase("they own").thenReturn((Selector.ControllerClause)
                    new Selector.ControllerClause.Owns(Selector.ControllerClause.Who.THEY)));

    // ── Selector ───────────────────────────────────────────────────────

    /// Decomposed `(objectType, qualifiers, withClauses)` form of a
    /// [Selector.TypeExpression]. `objectType` is the [GameObjectType]
    /// the selector picks (`PERMANENT` by default); `qualifiers` are
    /// the type-axis matchers; `withClauses` carries hoistable with-
    /// clauses surfaced by [#orShape] when the trailing alternative's
    /// with-clauses distribute across the disjunction.
    private record TypeShape(
            GameObjectType objectType, List<Selector.Qualifier> qualifiers, List<Selector.WithClause> withClauses) {
        TypeShape(GameObjectType objectType, List<Selector.Qualifier> qualifiers) {
            this(objectType, qualifiers, List.of());
        }

        TypeShape with(GameObjectType newObjectType) {
            return new TypeShape(newObjectType, qualifiers, withClauses);
        }

        TypeShape plus(List<Selector.Qualifier> extra) {
            if (extra.isEmpty()) return this;
            var combined = new ArrayList<>(qualifiers);
            combined.addAll(extra);
            return new TypeShape(objectType, List.copyOf(combined), withClauses);
        }
    }

    /// Folds a positive [Selector.SingleType] into the
    /// `(objectType, qualifiers)` pair the new model uses. Card type
    /// and subtype atoms become `Types(IsCardType(...))` and
    /// `Types(IsSubtype(...))`; `OfRole` becomes `Status.COMMANDER`.
    private static TypeShape singleShape(Selector.SingleType st) {
        return switch (st) {
            case Selector.SingleType.OfGameObject(var g) -> new TypeShape(g, List.of());
            case Selector.SingleType.OfCard(var c) ->
                new TypeShape(
                        GameObjectType.PERMANENT, List.of(new Selector.Qualifier.Types(new TypeMatcher.IsCardType(c))));
            case Selector.SingleType.OfSubtype(var s) ->
                new TypeShape(
                        GameObjectType.PERMANENT, List.of(new Selector.Qualifier.Types(new TypeMatcher.IsSubtype(s))));
            case Selector.SingleType.OfRole(var ignored) ->
                new TypeShape(GameObjectType.PERMANENT, List.of(Selector.Qualifier.Status.COMMANDER));
            case Selector.SingleType.ObjectCard(var g, var c) ->
                new TypeShape(g, List.of(new Selector.Qualifier.Types(new TypeMatcher.IsCardType(c))));
            case Selector.SingleType.ObjectSubtype(var g, var s) ->
                new TypeShape(g, List.of(new Selector.Qualifier.Types(new TypeMatcher.IsSubtype(s))));
        };
    }

    /// Folds a list of [Selector.SingleType] (the legacy `Compound`
    /// payload) into a single shape. The last `OfGameObject` wins for
    /// `objectType`; otherwise the object type stays at the prior
    /// accumulator's value (typically `PERMANENT`).
    private static TypeShape compoundShape(List<Selector.SingleType> types) {
        var acc = new TypeShape(GameObjectType.PERMANENT, List.of());
        for (var st : types) {
            var part = singleShape(st);
            // OfGameObject produces (objectType, []) — adopt the new
            // object type, no qualifiers to add. Other variants
            // produce (PERMANENT, [single qualifier]) — keep the
            // object type, append the qualifier.
            if (part.qualifiers().isEmpty()) {
                acc = acc.with(part.objectType());
            } else {
                acc = acc.plus(part.qualifiers());
            }
        }
        return acc;
    }

    /// Decomposes a [Selector.TypeExpression] into a [TypeShape], or
    /// returns `null` if the expression is a multi-axis `Or` that the
    /// new single-`Selector` model can't represent without a higher-
    /// level [SelectorExpression.Or]. The `Or` arm returns null also
    /// when alternatives have per-branch qualifiers that aren't axis-
    /// foldable into a matcher `Any`.
    private static @Nullable TypeShape decompose(Selector.TypeExpression type) {
        return switch (type) {
            case Selector.TypeExpression.Single(var st) -> singleShape(st);
            case Selector.TypeExpression.Compound(var types) -> compoundShape(types);
            case Selector.TypeExpression.Or(var alts) -> orShape(alts);
        };
    }

    /// Fold an [Selector.TypeExpression.Or] into a single [TypeShape].
    /// The unified [TypeMatcher] lets the disjunction span axes
    /// freely — "creature or Vehicle" (card type + subtype) folds to
    /// `Types(Any[IsCardType(CREATURE), IsSubtype(VEHICLE)])`.
    /// Alternatives may carry shared non-type qualifiers (e.g.,
    /// `Colors(Not(BLACK))` distributing across every branch);
    /// they're hoisted to the shared qualifier list as long as every
    /// branch carries them. Branches that name distinct non-PERMANENT
    /// heads still reject — the trailing game-object must agree
    /// across all branches.
    private static @Nullable TypeShape orShape(List<Selector.TypeExpression.Or.Alternative> alts) {
        if (alts.isEmpty()) return null;
        // Only the trailing alternative may carry a with-clause —
        // oracle convention attaches a distributing with-clause to
        // the last term ("instant or sorcery spell with mana value 3
        // or less"). Mid-list with-clauses bind to one branch and
        // can't be flattened, so we reject those.
        for (var i = 0; i < alts.size() - 1; i++) {
            if (!alts.get(i).withClauses().isEmpty()) return null;
        }
        var trailingWithClauses = alts.getLast().withClauses();
        var shapes = new ArrayList<TypeShape>(alts.size());
        for (var alt : alts) {
            var s = decompose(alt.type());
            if (s == null) return null;
            shapes.add(s);
        }
        // Unify object types: if any branch has an explicit non-
        // PERMANENT object type, that's the shared one for the
        // disjunction. If multiple branches name distinct non-
        // PERMANENT object types, reject.
        GameObjectType objectType = GameObjectType.PERMANENT;
        for (var s : shapes) {
            if (s.objectType() == GameObjectType.PERMANENT) continue;
            if (objectType == GameObjectType.PERMANENT) {
                objectType = s.objectType();
            } else if (objectType != s.objectType()) {
                return null;
            }
        }
        // Per-branch non-type qualifiers (Colors, Status, Target,
        // etc.) must be shared identically across every branch; only
        // then can we hoist them onto the outer Selector.
        //
        // Special case — distribute alt 0's non-type qualifiers when
        // every later alternative carries no non-type qualifier of
        // its own AND every later alternative's type is a [Single]
        // (one-token type). This handles oracle phrasings where the
        // leading qualifier binds to the whole list rather than just
        // alt 0:
        //   "tapped artifact, creature, and land" — TAPPED applies
        //   to all three; alt 0 carries it because that's where the
        //   tokenization put it.
        // The Single-only restriction is the disambiguator from
        // cases like Feast of Dreams's "enchanted creature or
        // enchantment creature", where alt 1's Compound type signals
        // a complete noun phrase — there ENCHANTED really is per-
        // branch and we'd lose semantic precision by distributing.
        var sharedNonType = nonTypeQualifiers(
                alts.getFirst().qualifiers(), shapes.getFirst().qualifiers());
        var canDistributeAlt0 = !sharedNonType.isEmpty();
        for (var i = 1; i < shapes.size() && canDistributeAlt0; i++) {
            var nt = nonTypeQualifiers(alts.get(i).qualifiers(), shapes.get(i).qualifiers());
            if (!nt.isEmpty() || !(alts.get(i).type() instanceof Selector.TypeExpression.Single)) {
                canDistributeAlt0 = false;
            }
        }
        if (!canDistributeAlt0) {
            for (var i = 1; i < shapes.size(); i++) {
                var nt = nonTypeQualifiers(
                        alts.get(i).qualifiers(), shapes.get(i).qualifiers());
                if (!nt.equals(sharedNonType)) return null;
            }
        }
        // Collect each branch's type qualifiers as a flat list of
        // matchers, lifting any trailing `IsCardType` from the last
        // branch onto earlier branches that lack one. Oracle text
        // "Elf or Soldier creature" implicitly distributes "creature"
        // across both branches — without the lift, we'd emit
        // `Any[IsSubtype(ELF), All[IsSubtype(SOLDIER), CARDTYPE(CREATURE)]]`
        // which is structurally noisy and semantically asymmetric.
        var perBranch = new ArrayList<List<TypeMatcher>>(shapes.size());
        for (var s : shapes) {
            var matchers = new ArrayList<TypeMatcher>();
            for (var q : s.qualifiers()) {
                if (q instanceof Selector.Qualifier.Types(var m)) matchers.add(m);
            }
            perBranch.add(matchers);
        }
        liftTrailingCardType(perBranch);
        var emptyBranches = (int) perBranch.stream().filter(List::isEmpty).count();
        if (emptyBranches > 0 && emptyBranches < perBranch.size()) return null;
        if (emptyBranches == perBranch.size()) return null;
        // Factor out matchers shared by every branch — common axes
        // distribute as a single outer `All`, with the differing axes
        // as the inner `Any`. "Elf or Soldier creature" →
        // `All[Any[IsSubtype(ELF), IsSubtype(SOLDIER)], IsCardType(CREATURE)]`.
        var common = new ArrayList<>(perBranch.getFirst());
        for (var i = 1; i < perBranch.size(); i++) {
            common.retainAll(perBranch.get(i));
        }
        var diffs = new ArrayList<TypeMatcher>();
        for (var matchers : perBranch) {
            var unique = new ArrayList<>(matchers);
            unique.removeAll(common);
            if (unique.isEmpty()) {
                diffs.add(null); // sentinel for "branch matches everything in common"
            } else if (unique.size() == 1) {
                diffs.add(unique.getFirst());
            } else {
                diffs.add(new TypeMatcher.All(List.copyOf(unique)));
            }
        }
        // If any branch is fully covered by `common` alone, the Any
        // collapses to vacuous-true on that branch — meaning the
        // disjunction is just `common`. Drop the Any.
        var anyVacuous = diffs.stream().anyMatch(d -> d == null);
        var combined = new ArrayList<>(sharedNonType);
        TypeMatcher unified;
        if (anyVacuous || diffs.isEmpty()) {
            unified = wrapAll(common);
        } else if (common.isEmpty()) {
            unified = diffs.size() == 1 ? diffs.getFirst() : new TypeMatcher.Any(List.copyOf(diffs));
        } else {
            var any = diffs.size() == 1 ? diffs.getFirst() : new TypeMatcher.Any(List.copyOf(diffs));
            var allParts = new ArrayList<TypeMatcher>();
            allParts.add(any);
            allParts.addAll(common);
            unified = new TypeMatcher.All(List.copyOf(allParts));
        }
        if (unified == null) return null;
        combined.add(new Selector.Qualifier.Types(unified));
        return new TypeShape(objectType, List.copyOf(combined), trailingWithClauses);
    }

    /// Distribute any [TypeMatcher.IsCardType] present in the LAST
    /// branch onto earlier branches that lack one. Implements the
    /// oracle-text convention that the trailing card type is shared
    /// across the disjunction ("Elf or Soldier creature" → "(Elf
    /// creature) or (Soldier creature)"). Only lifts onto branches
    /// that already carry at least one type matcher — bare game-
    /// object branches (e.g., "player" in "creature, player, or
    /// planeswalker") aren't a "(player) creature" intent and
    /// shouldn't get the trailing card type grafted on.
    private static void liftTrailingCardType(List<List<TypeMatcher>> perBranch) {
        if (perBranch.size() < 2) return;
        var last = perBranch.getLast();
        var trailingCardTypes =
                last.stream().filter(m -> m instanceof TypeMatcher.IsCardType).toList();
        if (trailingCardTypes.isEmpty()) return;
        for (var i = 0; i < perBranch.size() - 1; i++) {
            var branch = perBranch.get(i);
            if (branch.isEmpty()) continue;
            var hasCardType = branch.stream().anyMatch(m -> m instanceof TypeMatcher.IsCardType);
            if (hasCardType) continue;
            var lifted = new ArrayList<>(branch);
            lifted.addAll(trailingCardTypes);
            perBranch.set(i, lifted);
        }
    }

    private static @Nullable TypeMatcher wrapAll(List<TypeMatcher> matchers) {
        if (matchers.isEmpty()) return null;
        if (matchers.size() == 1) return matchers.getFirst();
        return new TypeMatcher.All(List.copyOf(matchers));
    }

    /// Collects qualifiers that aren't on the type axis (i.e., not
    /// [Selector.Qualifier.Types]) from the branch's per-alternative
    /// qualifier list and from the decomposed shape. Used by
    /// [#orShape] to detect qualifiers that must be shared
    /// identically across every Or branch before they can be hoisted
    /// onto the outer Selector.
    private static List<Selector.Qualifier> nonTypeQualifiers(
            List<Selector.Qualifier> branchQs, List<Selector.Qualifier> shapeQs) {
        var out = new ArrayList<Selector.Qualifier>();
        for (var q : branchQs) {
            if (!(q instanceof Selector.Qualifier.Types)) out.add(q);
        }
        for (var q : shapeQs) {
            if (!(q instanceof Selector.Qualifier.Types)) out.add(q);
        }
        return out;
    }

    /// Hoists the `target` qualifier from the first alternative onto
    /// the outer selector's shared qualifier list, then decomposes the
    /// type expression into `objectType + qualifiers`. Returns null
    /// if the type expression is a multi-axis Or that the new model
    /// can't represent — the wrapping `suchThat` then rejects the
    /// parse.
    private static @Nullable Selector hoistTarget(Selector.Quantifier quant, Selector.TypeExpression type) {
        var hoistedTarget = false;
        var workingType = type;
        if (type instanceof Selector.TypeExpression.Or(var alts) && !alts.isEmpty()) {
            var first = alts.getFirst();
            if (first.qualifiers().contains(Selector.Qualifier.TARGET)) {
                var stripped = first.qualifiers().stream()
                        .filter(q -> q != Selector.Qualifier.TARGET)
                        .toList();
                var newAlts = new ArrayList<>(alts);
                newAlts.set(0, new Selector.TypeExpression.Or.Alternative(stripped, first.type(), first.withClauses()));
                workingType = new Selector.TypeExpression.Or(List.copyOf(newAlts));
                hoistedTarget = true;
            }
        }
        var shape = decompose(workingType);
        if (shape == null) return null;
        var quals = new ArrayList<Selector.Qualifier>();
        if (hoistedTarget) quals.add(Selector.Qualifier.TARGET);
        quals.addAll(shape.qualifiers());
        return new Selector(quant, mergeAllAxes(quals), shape.objectType(), shape.withClauses(), null);
    }

    /// Runs every same-axis merge fold over the final qualifier list.
    /// The qualifier-list parser ([#QUALIFIER_LIST]) already folds the
    /// qualifiers it parses directly, but the Selector-building
    /// helpers append type qualifiers after that — so we re-run the
    /// folds here to collapse adjacent same-axis qualifiers (e.g.,
    /// "artifact creature" → two `Types(IsCardType(...))` collapsing
    /// into `Types(All[...])`).
    private static List<Selector.Qualifier> mergeAllAxes(List<Selector.Qualifier> qs) {
        qs = ColorQualifierParsers.mergeColorQualifiers(qs);
        qs = TypeQualifierParsers.mergeTypeQualifiers(qs);
        return qs;
    }

    /// A single-branch selector body — the non-Or case where one
    /// [#OR_ALTERNATIVE] describes the selector tail. Qualifiers and
    /// with-clauses become the outer [Selector]'s.
    private static @Nullable Selector flatFromAlt(
            Selector.Quantifier quant, Selector.TypeExpression.Or.Alternative alt) {
        var shape = decompose(alt.type());
        if (shape == null) return null;
        var combined = new ArrayList<Selector.Qualifier>(
                alt.qualifiers().size() + shape.qualifiers().size());
        combined.addAll(alt.qualifiers());
        combined.addAll(shape.qualifiers());
        return new Selector(quant, mergeAllAxes(combined), shape.objectType(), alt.withClauses(), null);
    }

    /// Multi-alternative type expression: [#OR_TYPE] / [#AND_TYPE] /
    /// [#AND_OR_TYPE] all yield a
    /// [Selector.TypeExpression.Or] and are interchangeable at the
    /// selector-body level. AND_OR is tried first because its literal
    /// "and/or" is a longer match than "and" or "or" alone.
    private static final Parser<Selector.TypeExpression> MULTI_ALT_TYPE =
            anyOf(AND_OR_TYPE, OR_TYPE, AND_TYPE, QUALIFIER_OR_WITH_OBJECT);

    private static final Parser<Selector> BASE_SELECTOR_OR = sequence(
                    QUANTIFIER, MULTI_ALT_TYPE, SelectorParsers::hoistTarget)
            .suchThat(s -> s != null, "decomposable or-selector");

    private static final Parser<Selector> BASE_SELECTOR_ALT = sequence(
                    QUANTIFIER, OR_ALTERNATIVE, SelectorParsers::flatFromAlt)
            .suchThat(s -> s != null, "decomposable single-alt selector");

    private static final Parser<Selector> BARE_SELECTOR_OR = MULTI_ALT_TYPE
            .map(or -> hoistTarget(Selector.Quantifier.one(), or))
            .suchThat(s -> s != null, "decomposable bare or-selector");

    private static final Parser<Selector> BARE_SELECTOR_ALT = OR_ALTERNATIVE
            .map(alt -> flatFromAlt(Selector.Quantifier.one(), alt))
            .suchThat(s -> s != null, "decomposable bare single-alt selector");

    /// "\[qualifiers\]? <q1> or <q2> <game-object>" — qualifier-prefixed
    /// distributive selector (Stifle: "target activated or triggered
    /// ability"). The prefix qualifier list is hoisted onto the
    /// Selector's shared qualifiers; each alternative inside the Or
    /// carries its own ability-source qualifier.
    /// Special-cased flattening for [#QUALIFIER_OR_WITH_OBJECT]:
    /// "activated or triggered ability" parses as an Or where each
    /// alternative carries a different per-branch
    /// [Selector.Qualifier.AbilitySource]. The new model can't fold
    /// these via the matcher-Any path (no AbilitySource matcher
    /// type), so we collect the per-branch qualifiers as a flat list
    /// alongside the shared game-object — downstream consumers
    /// interpret multiple `AbilitySource` qualifiers as disjunction.
    private static @Nullable Selector flattenQualifierOrWithObject(
            List<Selector.Qualifier> outerQuals, Selector.TypeExpression type) {
        if (!(type instanceof Selector.TypeExpression.Or(var alts))) return null;
        if (alts.isEmpty()) return null;
        var objectType = GameObjectType.PERMANENT;
        var combined = new ArrayList<>(outerQuals);
        for (var alt : alts) {
            var shape = decompose(alt.type());
            if (shape == null) return null;
            if (shape.objectType() != GameObjectType.PERMANENT) {
                if (objectType == GameObjectType.PERMANENT) objectType = shape.objectType();
                else if (objectType != shape.objectType()) return null;
            }
            combined.addAll(alt.qualifiers());
            combined.addAll(shape.qualifiers());
        }
        return new Selector(Selector.Quantifier.one(), mergeAllAxes(combined), objectType);
    }

    private static final Parser<Selector> QUALIFIER_PREFIX_QUALIFIER_OR_SELECTOR = sequence(
                    QUALIFIER_LIST, QUALIFIER_OR_WITH_OBJECT, SelectorParsers::flattenQualifierOrWithObject)
            .suchThat(s -> s != null, "decomposable qualifier-prefix or-selector");

    /// Bare "activated or triggered ability" — no leading qualifier
    /// list. Uses the same flatten path as
    /// [#QUALIFIER_PREFIX_QUALIFIER_OR_SELECTOR], with an empty outer
    /// qualifier list.
    private static final Parser<Selector> BARE_QUALIFIER_OR_SELECTOR = QUALIFIER_OR_WITH_OBJECT
            .map(type -> flattenQualifierOrWithObject(List.of(), type))
            .suchThat(s -> s != null, "decomposable bare qualifier-or selector");

    private static final Parser<Selector> CORE_SELECTOR = anyOf(
            QUALIFIER_PREFIX_QUALIFIER_OR_SELECTOR,
            BARE_QUALIFIER_OR_SELECTOR,
            BASE_SELECTOR_OR, // multi-branch must precede single-alt
            BASE_SELECTOR_ALT,
            BARE_SELECTOR_OR,
            BARE_SELECTOR_ALT);

    /// "in [possessive] [zone]" or "in [plural-zone]" — trailing zone scope
    /// on a selector ("cards in your hand", "cards in graveyards").
    /// Also handles the battlefield-specific "on the battlefield"
    /// idiom (Clone: "a copy of any creature on the battlefield").
    private static final Parser<Zone.Named> ZONE_CLAUSE = anyOf(
            // Multi-word possessives first so longer matches win.
            sequence(
                    phrase("in").then(anyOf(phrase("an opponent's"), phrase("each opponent's"))),
                    ZONE_NAME,
                    Zone.Named::new),
            // "in your opponents' <zone>s" — collective opponents'
            // plural possessive (Wight of Precinct Six: "for each
            // creature card in your opponents' graveyards.").
            sequence(
                    phrase("in your opponents'"),
                    PLURAL_ZONE_NAME,
                    (_, zone) -> new Zone.Named("your opponents'", zone)),
            phrase("in [your|their|its|a|any]").then(ZONE_NAME).map(Zone.Named::new),
            // "in [that|target] [player|opponent]'s <zone>" —
            // possessive on a named player (Storm Seeker: "the
            // number of cards in that player's hand").
            sequence(
                    phrase("in")
                            .then(anyOf(
                                    phrase("that player").thenReturn("that player"),
                                    phrase("that opponent").thenReturn("that opponent"),
                                    phrase("target player").thenReturn("target player"),
                                    phrase("target opponent").thenReturn("target opponent"),
                                    phrase("each player").thenReturn("each player"),
                                    phrase("each opponent").thenReturn("each opponent"),
                                    phrase("the chosen player").thenReturn("the chosen player")))
                            .followedBy(string("'s")),
                    ZONE_NAME,
                    (poss, zone) -> new Zone.Named(poss + "'s", zone)),
            phrase("in")
                    .then(anyOf(word("all").then(PLURAL_ZONE_NAME), PLURAL_ZONE_NAME))
                    .map(z -> new Zone.Named(null, z)),
            // "in each \[zone\]" — distributive every-zone scope, e.g.,
            // Rite of Flame's "for each card named ~ in each graveyard".
            // Treated as a possessive-less zone name so downstream
            // consumers read it as a bulk scope.
            phrase("in each").then(ZONE_NAME).map(z -> new Zone.Named("each", z)),
            phrase("on the battlefield").thenReturn(new Zone.Named(null, ZoneName.BATTLEFIELD)));

    /// "played by [player]" — cast-history participle (e.g., Uphill Battle:
    /// "Creatures played by your opponents enter tapped."). Captures the
    /// player phrase as free text bounded by [#WITH_STOP_WORDS] to
    /// avoid pulling in the trailing effect verb.
    private static final Parser<Selector.ThatClause> PLAYED_BY = phrase("played by")
            .then(CONTRACTION_WORD
                    .suchThat(w -> !WITH_STOP_WORDS.contains(w.toLowerCase()), "played-by word")
                    .atLeastOnce()
                    .map(words -> String.join(" ", words)))
            .map(s -> new Selector.ThatClause.Predicate("played by " + s));

    /// "of the [card type | creature type | color] of [owner]'s choice"
    /// — selector modifier naming a category chosen by the player
    /// (Extinction: "Destroy all creatures of the creature type of your
    /// choice."). "of the chosen \[category\]" is a back-reference to
    /// an earlier "Choose a \[category\]" (Sudden Demise: "Choose a
    /// color. … each creature of the chosen color."). The chosen
    /// dimension is captured as free text.
    private static final Parser<Selector.ThatClause> OF_CHOICE_CATEGORY = anyOf(
            phrase("of the")
                    .then(anyOf(
                            phrase("creature type"),
                            phrase("card type"),
                            phrase("color"),
                            phrase("land type"),
                            phrase("subtype")))
                    .followedBy(phrase("of [your|their|its|an|any] choice"))
                    .map(category -> new Selector.ThatClause.Predicate("of the " + category + " of <owner>'s choice")),
            // "of the chosen <category>" — back-reference to an
            // earlier "Choose a …" effect (Sudden Demise).
            phrase("of the chosen")
                    .then(anyOf(
                            phrase("creature type"),
                            phrase("card type"),
                            word("color"),
                            phrase("land type"),
                            word("subtype")))
                    .map(category -> new Selector.ThatClause.Predicate("of the chosen " + category)),
            // "of that <category>" — shorthand back-reference to an
            // earlier "Choose a …" effect (Persecute: "Choose a color.
            // Target player … discards all cards of that color.").
            phrase("of that")
                    .then(anyOf(
                            phrase("creature type"),
                            phrase("card type"),
                            word("color"),
                            phrase("land type"),
                            word("subtype")))
                    .map(category -> new Selector.ThatClause.Predicate("of that " + category)),
            // "of [poss] choice" — direct selector-level chooser (Pay No
            // Heed: "a source of your choice"; Clip Wings: "a creature of
            // their choice").
            phrase("of")
                    .then(anyOf(word("your"), word("their"), word("its"), word("an"), word("any")))
                    .followedBy(word("choice"))
                    .map(poss -> new Selector.ThatClause.Predicate("of " + poss + " choice")));

    /// Forward-declared rule tying back to [#SELECTOR] so participles
    /// like [#ATTACHED_TO] can nest a full selector inside themselves
    /// (breaking the static-init cycle between the outer SELECTOR and its
    /// inner participle clauses).
    private static final Parser.Rule<Selector> SELECTOR_RULE = new Parser.Rule<>();

    /// Forward-declared rule for [#PARTICIPIAL_CLAUSE] so other
    /// parser modules (e.g., [SubjectParsers]) can reference it
    /// without triggering a static-init cycle.
    static final Parser.Rule<Selector.ThatClause> PARTICIPIAL_CLAUSE_RULE = new Parser.Rule<>();

    /// "attached to [selector|pronoun]" — attachment participle (Devout
    /// Harpist: "Destroy target Aura attached to a creature."; Miracle
    /// Worker: "attached to a creature you control"; Graceblade Artisan:
    /// "for each Aura attached to it."). Accepts a bare pronoun ("it" /
    /// "them" / "itself") in addition to a full [#SELECTOR] so the
    /// pronoun-referenced form doesn't fall through to SELECTOR and leave
    /// the pronoun unconsumed.
    private static final Parser<Selector.ThatClause> ATTACHED_TO = phrase("attached to")
            .then(anyOf(
                    word("it"),
                    word("them"),
                    word("itself"),
                    // "that creature" / "that permanent" — demonstrative
                    // back-reference to a subject named earlier in the
                    // clause (Eaten by Spiders: "Destroy target creature
                    // with flying and all Equipment attached to that
                    // creature.").
                    phrase("[that|this] [creature|permanent|card|Equipment|Aura]"),
                    SELECTOR_RULE.map(Object::toString)))
            .map(s -> new Selector.ThatClause.Predicate("attached to " + s));

    /// "cast from [zone]" — origin-zone participle on spells (e.g.,
    /// Laquatus's Disdain: "Counter target spell cast from a graveyard.").
    /// The zone is captured as `[article] <zone-name>`.
    private static final Parser<Selector.ThatClause> CAST_FROM_PARTICIPLE = sequence(
            phrase("cast from")
                    .then(anyOf(word("a"), word("an"), word("the"), word("your"), word("their"), word("its"))),
            ZONE_NAME,
            (poss, zone) -> new Selector.ThatClause.Predicate(
                    "cast from " + poss + " " + zone.name().toLowerCase()));

    /// "from a\[n\] \[card-type\] source" — origin-source restriction on
    /// an ability-target selector (Rust: "Counter target activated
    /// ability from an artifact source."). Yields a structured
    /// [Selector.ThatClause.FromSourceOfType].
    private static final Parser<Selector.ThatClause> FROM_SOURCE_PARTICIPLE = phrase("from a(n)")
            .then(CARD_TYPE)
            .followedBy(word("source"))
            .map(Selector.ThatClause.FromSourceOfType::new);

    /// "blocking [subject]" — directed-block participle (e.g., Knight of
    /// Dusk: "Destroy target creature blocking this creature."). Captures
    /// the target as free text bounded by [#WITH_STOP_WORDS] so we
    /// avoid a static-init cycle with [SubjectParsers]. Tried before
    /// the bare "blocking" participle so the longer match wins.
    private static final Parser<Selector.ThatClause> BLOCKING_SUBJECT = phrase("blocking")
            .then(CONTRACTION_WORD
                    .suchThat(w -> !WITH_STOP_WORDS.contains(w.toLowerCase()), "blocking-subject word")
                    .atLeastOnce()
                    .map(words -> String.join(" ", words)))
            .map(s -> new Selector.ThatClause.Predicate("blocking " + s));

    /// Participial suffix (`attacking you`, `blocking`, `attacking or
    /// blocking`, `played by X`, `attached to X`) without an explicit
    /// `that is …`. Oracle text attaches these directly to a type:
    /// `creature attacking you` = `creature that is attacking you`.
    /// Rendered into a [Selector.ThatClause].
    static final Parser<Selector.ThatClause> PARTICIPIAL_CLAUSE = anyOf(
            PLAYED_BY,
            ATTACHED_TO,
            CAST_FROM_PARTICIPLE,
            FROM_SOURCE_PARTICIPLE,
            OF_CHOICE_CATEGORY,
            phrase("attacking you").map(Selector.ThatClause.Predicate::new),
            phrase("attacking or blocking").map(Selector.ThatClause.Predicate::new),
            phrase("attacking").map(Selector.ThatClause.Predicate::new),
            BLOCKING_SUBJECT, // must precede the bare "blocking"
            phrase("blocking").map(Selector.ThatClause.Predicate::new),
            phrase("blocked").map(Selector.ThatClause.Predicate::new),
            phrase("unblocked").map(Selector.ThatClause.Predicate::new),
            // "dealt damage by \[self-ref\] this turn" — damage-history
            // participle where the damage source is a self-reference
            // (Wicked Akuba: "Target player dealt damage by this
            // creature this turn loses 1 life."). Must precede the
            // bare "dealt damage" forms so the longer match wins.
            // Restricted to self-reference sources so this clause
            // doesn't nest the full [SubjectParsers#SUBJECT] grammar
            // — that would form a static-init cycle.
            phrase("dealt damage by")
                    .then(anyOf(
                            string("~"),
                            word("this")
                                    .then(anyOf(word("creature"), word("permanent"), word("card")))
                                    .thenReturn("this creature"),
                            word("it").thenReturn("it")))
                    .followedBy(phrase("this turn"))
                    .map(src -> new Selector.ThatClause.Predicate("dealt damage by " + src + " this turn")),
            // "dealt damage this turn" / "dealt damage" — damage-history
            // participle (Inflame: "each creature dealt damage this
            // turn.").
            phrase("dealt damage this turn").map(Selector.ThatClause.Predicate::new),
            phrase("dealt damage").map(Selector.ThatClause.Predicate::new),
            // "countered this way" — counter-history participle (Swift
            // Silence: "Draw a card for each spell countered this way.").
            phrase("countered this way").map(Selector.ThatClause.Predicate::new),
            // "of \[that|the chosen\] type" — type back-reference to
            // a preceding [Effect.ChooseType] effect (Distant Melody:
            // "each permanent you control of that type.").
            phrase("of [that|the chosen] type").thenReturn(Selector.ThatClause.ReferencedType.REFERENCED_TYPE),
            // "destroyed this way" — destroy-history participle
            // (Fumigate: "You gain 1 life for each creature destroyed
            // this way.").
            phrase("destroyed this way").map(Selector.ThatClause.Predicate::new),
            // "discarded this way" — discard-history participle
            // (Syphon Mind: "You draw a card for each card discarded
            // this way.").
            phrase("discarded this way").map(Selector.ThatClause.Predicate::new),
            // "exiled this way" — exile-history participle (Martyr's
            // Cry: "For each creature exiled this way, …").
            phrase("exiled this way").map(Selector.ThatClause.Predicate::new),
            // "sacrificed this way" — sacrifice-history participle
            // (Renounce: "for each permanent sacrificed this way.").
            phrase("sacrificed this way").map(Selector.ThatClause.Predicate::new),
            // "cast this turn" — past-tense cast participle (Storm
            // Entity: "for each other spell cast this turn."). The
            // controller is implicit (any caster) until a card needs
            // a tighter binding.
            phrase("cast this turn").map(Selector.ThatClause.Predicate::new),
            // "drawn this way" — draw-history participle (Read the
            // Runes: "For each card drawn this way, …").
            phrase("drawn this way").map(Selector.ThatClause.Predicate::new),
            // "you put into your graveyard this way" — past-action
            // participle for manifest-dread (Paranormal Analyst:
            // "Whenever you manifest dread, put a card you put into
            // your graveyard this way into your hand."). The "this
            // way" anchors to the manifest-dread resolution context.
            phrase("you put into your graveyard this way")
                    .<Selector.ThatClause>thenReturn(
                            new Selector.ThatClause.Predicate("you put into your graveyard this way")),
            // "other than \[~\|this creature\|this permanent\|this card\]"
            // — exclusion of the ability's source (Demonic
            // Taskmaster: "sacrifice a creature other than this
            // creature.").
            phrase("other than")
                    .then(anyOf(
                            string("~"),
                            phrase("this [creature|permanent|card]"),
                            // "target player" / "target opponent" —
                            // Death by Dragons ("Each player other than
                            // target player creates a 5/5 …"). Inline
                            // player-ref to avoid a static-init cycle
                            // with SubjectParsers.
                            phrase("target player"),
                            phrase("target opponent")))
                    .map(ref -> new Selector.ThatClause.Predicate("other than " + ref)),
            // "named X" — name-equality clause (Powerstone Shard: "each
            // artifact you control named Powerstone Shard."; Gisela,
            // the Broken Blade: "a creature named Bruna, the Fading
            // Light"). Self-reference substitution has already replaced
            // the card's own name with "~"; we accept that as a literal
            // alternate. Otherwise consumes a multi-word card name via
            // [CardNameParsers#CARD_NAME] so legendary epithets with
            // commas land in a single typed clause.
            phrase("named").then(anyOf(string("~"), CardNameParsers.CARD_NAME)).map(Selector.ThatClause.NamedAs::new),
            // "you drew this turn" — draw-history participle (Jandor's
            // Ring: "the last card you drew this turn"). Currently the
            // clause text is captured verbatim; the controller can be
            // tightened later if needed.
            phrase("you drew this turn").map(Selector.ThatClause.Predicate::new),
            // "\[player-ref\] discarded this turn" — discard-history
            // participle identifying the discarding player (Dream
            // Salvage: "cards target opponent discarded this turn").
            // Uses an inline mini-parser for the player word so this
            // clause doesn't trigger a static-init cycle with
            // [SubjectParsers].
            sequence(
                    anyOf(
                            phrase("target opponent"),
                            phrase("target player"),
                            phrase("each opponent"),
                            phrase("each player"),
                            phrase("that player"),
                            phrase("You").thenReturn("you"),
                            phrase("They").thenReturn("they")),
                    anyOf(
                            // "cycled or discarded this turn" — Shadow of
                            // the Grave. Oxford form; must precede the
                            // bare "discarded" so the longer match wins.
                            phrase("cycled or discarded this turn").thenReturn("cycled or discarded this turn"),
                            phrase("discarded this turn").thenReturn("discarded this turn"),
                            // Present-perfect contracted form (Knowledge
                            // Is Power: "the number of cards you've
                            // drawn this turn").
                            phrase("['ve|has|have] drawn this turn").thenReturn("drawn this turn")),
                    (ref, verb) -> new Selector.ThatClause.Predicate(ref.toLowerCase() + " " + verb)),
            // "who \[doesn't\]? control \[selector\]" — player-specific
            // control participle (Thornbow Archer: "each opponent who
            // doesn't control an Elf"). Uses SELECTOR_RULE so the
            // nested selector can reuse the full grammar without
            // triggering a static-init cycle.
            sequence(
                    phrase("who")
                            .then(anyOf(
                                    phrase("doesn't control").thenReturn(true),
                                    phrase("controls").thenReturn(false))),
                    SELECTOR_RULE,
                    (negated, sel) -> new Selector.ThatClause.Predicate(
                            "who " + (negated ? "doesn't control " : "controls ") + sel)),
            // "who drew a card this way" — back-reference participle to
            // a preceding Draw clause in the same resolution (Kwain,
            // Itinerant Meddler: "Each player may draw a card, then each
            // player who drew a card this way gains 1 life.").
            phrase("who drew a card this way").map(Selector.ThatClause.Predicate::new),
            // "who attacked this turn" — combat-history relative
            // clause on a player target (Fire and Brimstone: "deals
            // 4 damage to target player who attacked this turn").
            phrase("who attacked this turn").map(Selector.ThatClause.Predicate::new));

    /// "except for <type>" — trailing exclusion clause (Slash the Ranks:
    /// "Destroy all creatures and planeswalkers except for commanders.").
    /// Stored as a negated [Selector.WithClause] so the existing
    /// with-clause channel carries both inclusion and exclusion filters.
    private static final Parser<Selector.WithClause> EXCEPT_CLAUSE = phrase("except for")
            .then(anyOf(
                    // "except for [selector]" — full selector
                    // exclusion (Flame Sweep: "each creature except
                    // for creatures you control with flying.").
                    // Uses SELECTOR_RULE for the nested selector.
                    SELECTOR_RULE.<String>map(Object::toString),
                    word().suchThat(w -> !WITH_STOP_WORDS.contains(w.toLowerCase()), "except-clause word")
                            .atLeastOnce()
                            .map(words -> String.join(" ", words))))
            .map(text -> (Selector.WithClause) new Selector.WithClause.HasPredicate(true, "except for " + text));

    /// Words that bound a card-name capture — once we hit one of
    /// these the [#NAME_TOKEN] parser yields control to the outer
    /// grammar so trailing zone / predicate clauses parse correctly
    /// (Life Burst: "card named Life Burst in each graveyard").
    private static final Set<String> NAME_STOP_WORDS =
            Set.of("in", "with", "that", "from", "to", "and", "or", "into", "you", "your", "their");

    private static final Parser<String> NAME_TOKEN = anyOf(
            Parser.string("~"),
            Parser.word().suchThat(w -> !NAME_STOP_WORDS.contains(w.toLowerCase()), "card-name token"));

    private static final Parser<String> CARD_NAME = NAME_TOKEN.atLeastOnce().map(ws -> String.join(" ", ws));

    /// Trailing "[not]? named \<card-name\>" predicate — Clever
    /// Conjurer: "target permanent not named Clever Conjurer." The
    /// card name is literal (oracle text has it substituted to `~`
    /// for self-reference). Bounded by [#NAME_STOP_WORDS] so trailing
    /// "in <zone>" / "with <predicate>" tails aren't swallowed.
    private static final Parser<Selector.WithClause> NAMED_CLAUSE = anyOf(
            phrase("not named").then(CARD_NAME).map(name ->
                    (Selector.WithClause) new Selector.WithClause.HasName(true, name)),
            phrase("named").then(CARD_NAME).map(name ->
                    (Selector.WithClause) new Selector.WithClause.HasName(false, name)));

    /// Trailing "of each \<axis\>" qualifier — Coalition Victory: "a
    /// land of each basic land type", "a creature of each color".
    /// Folded into the selector's qualifier list via
    /// [Selector#addQualifier].
    private static final Parser<Selector.Qualifier> OF_EACH_CLAUSE = phrase("of each")
            .then(Parser.<Selector.Qualifier.CoverageAxis>anyOf(
                    phrase("basic land type").thenReturn(Selector.Qualifier.CoverageAxis.BASIC_LAND_TYPE),
                    phrase("color").thenReturn(Selector.Qualifier.CoverageAxis.COLOR)))
            .map(Selector.Qualifier.OfEach::new);

    public static final Parser<Selector> SELECTOR = CORE_SELECTOR
            .optionallyFollowedBy(OF_EACH_CLAUSE, Selector::addQualifier)
            .optionallyFollowedBy(NAMED_CLAUSE, Selector::addWithClause)
            .optionallyFollowedBy(CONTROLLER_CLAUSE, Selector::withController)
            .optionallyFollowedBy(WITH_CLAUSE, Selector::withWithClause)
            // Trailing controller-clause after a with-clause lets the
            // `type with X you own` order parse too (Runic Repetition:
            // "target exiled card with flashback you own"), without
            // forcing oracle text to front-load the controller.
            .optionallyFollowedBy(CONTROLLER_CLAUSE, Selector::withController)
            .optionallyFollowedBy(THAT_CLAUSE, Selector::withThatClause)
            .optionallyFollowedBy(PARTICIPIAL_CLAUSE, Selector::withThatClause)
            // Allow a trailing with-clause after a that/participial clause
            // too (Clip Wings: "a creature of their choice with flying").
            .optionallyFollowedBy(WITH_CLAUSE, Selector::withWithClause)
            .optionallyFollowedBy(EXCEPT_CLAUSE, Selector::addWithClause)
            .optionallyFollowedBy(ZONE_CLAUSE, Selector::withZone)
            // A that-clause can also trail the zone clause (Shadow of
            // the Grave: "all cards in your graveyard that you cycled
            // or discarded this turn.") — the "in <zone>" qualifier
            // must come first, the "that …" predicate narrows the
            // zone contents.
            .optionallyFollowedBy(THAT_CLAUSE, Selector::withThatClause)
            // A with-clause can also trail the zone clause (Radiant,
            // Archangel: "for each other creature on the battlefield
            // with flying.") — the zone narrows location, the
            // with-clause narrows the abilities of the matching set.
            .optionallyFollowedBy(WITH_CLAUSE, Selector::withWithClause);

    static {
        SELECTOR_RULE.definedAs(SELECTOR);
        PARTICIPIAL_CLAUSE_RULE.definedAs(PARTICIPIAL_CLAUSE);
    }
}
