package be.imgn.mtg.engine.oracle;

import static be.imgn.mtg.engine.oracle.Words.anyCiWord;
import static be.imgn.mtg.engine.oracle.Words.anyWord;
import static be.imgn.mtg.engine.oracle.Words.ciWords;
import static be.imgn.mtg.engine.oracle.Words.phrase;
import static be.imgn.mtg.engine.oracle.Words.w;
import static be.imgn.mtg.engine.oracle.Words.words;
import static com.google.common.labs.parse.Parser.anyOf;
import static com.google.common.labs.parse.Parser.consecutive;
import static com.google.common.labs.parse.Parser.digits;
import static com.google.common.labs.parse.Parser.or;
import static com.google.common.labs.parse.Parser.sequence;
import static com.google.common.labs.parse.Parser.string;
import static com.google.common.labs.parse.Parser.word;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import com.google.common.labs.parse.CharacterSet;
import com.google.common.labs.parse.Parser;

/// Parsers for selectors, types, amounts, and related noun-phrase grammar.
final class SelectorParsers {
    private SelectorParsers() {}

    // ── Primitive parsers ──────────────────────────────────────────────

    static final Parser<Integer> INTEGER = digits().map(Integer::parseInt);

    static final Parser<Integer> SIGNED_INT =
            sequence(anyOf(string("+").thenReturn(1), string("-").thenReturn(-1)), INTEGER, (sign, num) -> sign * num);

    static final Parser<Integer> WORD_NUMBER = anyOf(
            w("one").thenReturn(1),
            w("two").thenReturn(2),
            w("three").thenReturn(3),
            w("four").thenReturn(4),
            w("five").thenReturn(5),
            w("six").thenReturn(6),
            w("seven").thenReturn(7),
            w("eight").thenReturn(8),
            w("nine").thenReturn(9),
            w("ten").thenReturn(10),
            w("eleven").thenReturn(11),
            w("twelve").thenReturn(12),
            w("thirteen").thenReturn(13),
            w("fourteen").thenReturn(14),
            w("fifteen").thenReturn(15),
            w("sixteen").thenReturn(16),
            w("seventeen").thenReturn(17),
            w("eighteen").thenReturn(18),
            w("nineteen").thenReturn(19),
            w("twenty").thenReturn(20));

    /// An integer written either as digits (`3`) or an English word
    /// number (`three`). Prefer this when oracle text accepts both.
    public static final Parser<Integer> NUMBER = anyOf(WORD_NUMBER, INTEGER);

    // ── Amount ─────────────────────────────────────────────────────────

    /// "half [amount] [subject-word]? [, rounded up/down]?" — scalar
    /// halving with optional rounding (Contaminated Drink: "half X rad
    /// counters, rounded up"). Default rounding is UP. The trailing
    /// "rounded up/down" clause is produced as a suffix Parser<Amount> so
    /// it can be composed where this atom is used; here we handle only
    /// the bare "half [atom]" form — consumers that need the rounding
    /// suffix should compose explicitly.
    private static final Parser<Amount> HALF_ATOM = ciWords("half")
            .then(anyOf(
                    word("X").thenReturn(Amount.variable()),
                    WORD_NUMBER.map(Amount::exact),
                    INTEGER.map(Amount::exact)))
            .<Amount>map(base -> new Amount.Half(base, Amount.Half.Rounding.UP));

    /// "N or more" — lower-bound amount (Military Intelligence: "attack
    /// with two or more creatures"). Must precede bare number atoms so
    /// the "or more" tail isn't left for an outer "or".
    private static final Parser<Amount> AT_LEAST_ATOM =
            sequence(anyOf(WORD_NUMBER, INTEGER), ciWords("or more"), (n, _) -> (Amount) new Amount.AtLeast(n));

    /// "N or M" — range amount bounded on both sides (Storm of Steel:
    /// "each of one or two targets"). Tried alongside AT_LEAST_ATOM.
    private static final Parser<Amount> RANGE_ATOM =
            sequence(anyOf(WORD_NUMBER, INTEGER), w("or").then(anyOf(WORD_NUMBER, INTEGER)), (min, max) ->
                    (Amount) new Amount.Range(min, max));

    /// "up to N" — upper-bound amount (Render Inert: "Remove up to five
    /// counters from target permanent.").
    private static final Parser<Amount> UP_TO_ATOM =
            ciWords("up to").then(anyOf(WORD_NUMBER, INTEGER)).<Amount>map(Amount.UpTo::new);

    /// "twice [base]" / "N times [base]" — multiplicative atom (Boon
    /// Reflection: "you gain twice that much life."; Crackle with
    /// Power: "deals five times X damage to each of up to X
    /// targets."). The leading multiplier is either the irregular
    /// `twice` or a word-number/integer followed by `times`.
    private static final Parser<Amount> TIMES_ATOM = sequence(
            anyOf(w("twice").thenReturn(2), WORD_NUMBER.followedBy(word("times")), INTEGER.followedBy(word("times"))),
            anyOf(
                    word("that").then(anyWord("much", "many")).map(w -> Amount.reference("that " + w)),
                    word("X").thenReturn(Amount.variable()),
                    WORD_NUMBER.map(Amount::exact),
                    INTEGER.map(Amount::exact)),
            (n, base) -> (Amount) new Amount.Times(n, base));

    /// A single-term amount — the atom before the optional `plus` suffix.
    private static final Parser<Amount> ATOMIC_AMOUNT = anyOf(
            HALF_ATOM, // must precede INTEGER/WORD_NUMBER — "half" is a word.
            UP_TO_ATOM,
            TIMES_ATOM,
            AT_LEAST_ATOM, // must precede RANGE_ATOM (more specific "or more" tail).
            RANGE_ATOM, // must precede bare WORD_NUMBER/INTEGER so "N or M" wins.
            word("X").thenReturn(Amount.variable()),
            w("that").then(anyWord("much", "many")).map(w -> Amount.reference("that " + w)),
            WORD_NUMBER.map(Amount::exact),
            INTEGER.map(Amount::exact),
            anyCiWord("a", "an").thenReturn(Amount.exact(1)));

    /// Amount expression, optionally followed by `plus <atom>` for arithmetic
    /// like "X plus 3".
    public static final Parser<Amount> AMOUNT =
            ATOMIC_AMOUNT.optionallyFollowedBy(word("plus").then(ATOMIC_AMOUNT), Amount.Plus::new);

    // ── Enums ──────────────────────────────────────────────────────────

    public static final Parser<Color> COLOR = anyOf(
            w("white").thenReturn(Color.WHITE),
            w("blue").thenReturn(Color.BLUE),
            w("black").thenReturn(Color.BLACK),
            w("red").thenReturn(Color.RED),
            w("green").thenReturn(Color.GREEN));

    public static final Parser<CardType> CARD_TYPE = anyOf(
            anyCiWord("creatures", "creature").thenReturn(CardType.CREATURE),
            anyCiWord("artifacts", "artifact").thenReturn(CardType.ARTIFACT),
            anyCiWord("enchantments", "enchantment").thenReturn(CardType.ENCHANTMENT),
            anyCiWord("lands", "land").thenReturn(CardType.LAND),
            anyCiWord("planeswalkers", "planeswalker").thenReturn(CardType.PLANESWALKER),
            anyCiWord("battles", "battle").thenReturn(CardType.BATTLE),
            anyCiWord("instants", "instant").thenReturn(CardType.INSTANT),
            anyCiWord("sorceries", "sorcery").thenReturn(CardType.SORCERY),
            w("kindred").thenReturn(CardType.KINDRED),
            anyCiWord("dungeons", "dungeon").thenReturn(CardType.DUNGEON));

    public static final Parser<GameObjectType> GAME_OBJECT_TYPE = anyOf(
            anyCiWord("permanents", "permanent").thenReturn(GameObjectType.PERMANENT),
            anyCiWord("spells", "spell").thenReturn(GameObjectType.SPELL),
            anyCiWord("cards", "card").thenReturn(GameObjectType.CARD),
            anyCiWord("tokens", "token").thenReturn(GameObjectType.TOKEN),
            anyCiWord("sources", "source").thenReturn(GameObjectType.SOURCE),
            anyCiWord("abilities", "ability").thenReturn(GameObjectType.ABILITY),
            anyCiWord("players", "player").thenReturn(GameObjectType.PLAYER));

    public static final Parser<Supertype> SUPERTYPE = anyOf(
            w("legendary").thenReturn(Supertype.LEGENDARY),
            w("basic").thenReturn(Supertype.BASIC),
            w("snow").thenReturn(Supertype.SNOW),
            w("world").thenReturn(Supertype.WORLD));

    public static final Parser<ZoneName> ZONE_NAME = anyOf(
            w("battlefield").thenReturn(ZoneName.BATTLEFIELD),
            w("graveyard").thenReturn(ZoneName.GRAVEYARD),
            w("library").thenReturn(ZoneName.LIBRARY),
            w("hand").thenReturn(ZoneName.HAND),
            w("exile").thenReturn(ZoneName.EXILE),
            w("stack").thenReturn(ZoneName.STACK),
            ciWords("command zone").thenReturn(ZoneName.COMMAND));

    /// Plural forms of zones that cards reference collectively
    /// ("all graveyards", "all libraries", "all hands").
    public static final Parser<ZoneName> PLURAL_ZONE_NAME = anyOf(
            w("graveyards").thenReturn(ZoneName.GRAVEYARD),
            w("libraries").thenReturn(ZoneName.LIBRARY),
            w("hands").thenReturn(ZoneName.HAND));

    // ── Counter type ───────────────────────────────────────────────────

    private static final Parser<CounterType> PT_COUNTER =
            sequence(SIGNED_INT, string("/").then(SIGNED_INT), CounterType::ptCounter);

    private static final Parser<CounterType> NAMED_COUNTER = word().suchThat(
                    w -> !w.equals("counter") && !w.equals("counters"), "counter name")
            .map(CounterType::named);

    public static final Parser<CounterType> COUNTER_TYPE = anyOf(PT_COUNTER, NAMED_COUNTER);

    // ── P/T value ──────────────────────────────────────────────────────

    /// Atomic P/T value — a fixed integer or the variable "X".
    private static final Parser<Amount> PT_ATOM =
            anyOf(INTEGER.map(Amount::exact), word("X").thenReturn(Amount.variable()));

    public static final Parser<PtValue> PT_VALUE = sequence(PT_ATOM, string("/").then(PT_ATOM), PtValue::new);

    // ── Single type ────────────────────────────────────────────────────

    private static final Parser<Selector.SingleType> OBJECT_CARD_TYPE = sequence(
            GAME_OBJECT_TYPE.suchThat(
                    t -> t != GameObjectType.TOKEN && t != GameObjectType.SOURCE, "object type for compound"),
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

    static final Parser<Selector.SingleType> SINGLE_TYPE =
            anyOf(OBJECT_CARD_TYPE, CARD_SINGLE, OBJECT_SINGLE, SUBTYPE_SINGLE);

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
            w("all").thenReturn(Selector.Quantifier.all()),
            // "both" — exactly two; modelled as a fixed Count(2)
            // (Alaborn Zealot: "destroy both creatures").
            w("both").thenReturn(Selector.Quantifier.count(2)),
            w("each").thenReturn(Selector.Quantifier.each()),
            w("every").thenReturn(Selector.Quantifier.every()),
            w("another").thenReturn(Selector.Quantifier.another()),
            w("other").thenReturn(Selector.Quantifier.other()),
            w("the").thenReturn(Selector.Quantifier.the()),
            word("X").thenReturn(Selector.Quantifier.variable()),
            ciWords("up to").then(anyOf(WORD_NUMBER, INTEGER)).map(Selector.Quantifier::upTo),
            ciWords("any number of").thenReturn(Selector.Quantifier.anyNumber()),
            // "one or more" — at least one. Must precede the bare-integer
            // and "N or M" range arms so the literal prefix wins.
            ciWords("one or more").thenReturn(Selector.Quantifier.range(1, Integer.MAX_VALUE)),
            // "N or more" — at-least-N (Rampaging Ceratops: "except by
            // three or more creatures.").
            sequence(
                    anyOf(WORD_NUMBER, INTEGER),
                    words("or more"),
                    (n, _) -> Selector.Quantifier.range(n, Integer.MAX_VALUE)),
            // "N or M" — inclusive range. Tried before bare N so the trailing
            // " or M" isn't left for a downstream selector-level "or".
            sequence(
                    anyOf(WORD_NUMBER, INTEGER),
                    word("or").then(anyOf(WORD_NUMBER, INTEGER)),
                    Selector.Quantifier::range),
            WORD_NUMBER.map(Selector.Quantifier::count),
            INTEGER.suchThat(n -> n > 1, "count > 1").map(Selector.Quantifier::count),
            anyCiWord("a", "an").thenReturn(Selector.Quantifier.one()));

    // ── Qualifier ──────────────────────────────────────────────────────

    private static final Parser<Selector.Qualifier> TARGET_Q = w("target").thenReturn(Selector.Qualifier.TARGET);

    static final Parser<ColorFilter> COLOR_FILTER = Parser.<ColorFilter>anyOf(
            w("nonwhite").thenReturn(ColorFilter.NON_WHITE),
            w("nonblue").thenReturn(ColorFilter.NON_BLUE),
            w("nonblack").thenReturn(ColorFilter.NON_BLACK),
            w("nonred").thenReturn(ColorFilter.NON_RED),
            w("nongreen").thenReturn(ColorFilter.NON_GREEN),
            w("colorless").thenReturn(ColorFilter.COLORLESS),
            w("multicolored").thenReturn(ColorFilter.MULTICOLORED),
            w("monocolored").thenReturn(ColorFilter.MONOCOLORED),
            w("white").thenReturn(ColorFilter.WHITE),
            w("blue").thenReturn(ColorFilter.BLUE),
            w("black").thenReturn(ColorFilter.BLACK),
            w("red").thenReturn(ColorFilter.RED),
            w("green").thenReturn(ColorFilter.GREEN));

    /// "[color] [and/or [color]]? …" — single filter, Oxford or-list,
    /// or and/or-list of color filters. Emits `Color` for a single
    /// filter, `Colors` for a list (Evaporate: "white and/or blue
    /// creature").
    private static final Parser<Selector.Qualifier> COLOR_Q = anyOf(
            MtgParsers.andOrList(COLOR_FILTER)
                    .suchThat(l -> l.size() >= 2, "and/or list of colors")
                    .map(Selector.Qualifier.Colors::new),
            MtgParsers.orList(COLOR_FILTER)
                    .map(filters -> filters.size() == 1
                            ? Selector.Qualifier.color(filters.getFirst())
                            : new Selector.Qualifier.Colors(filters)));

    private static final Parser<Selector.Qualifier> SUPERTYPE_Q = SUPERTYPE.map(Selector.Qualifier::ofSupertype);

    private static final Parser<Selector.Qualifier> NEGATED_SUPERTYPE_Q = anyOf(
            w("nonlegendary").thenReturn(Selector.Qualifier.negatedSupertype(Supertype.LEGENDARY)),
            w("nonbasic").thenReturn(Selector.Qualifier.negatedSupertype(Supertype.BASIC)),
            w("nonsnow").thenReturn(Selector.Qualifier.negatedSupertype(Supertype.SNOW)));

    private static final Parser<Selector.Qualifier> NEGATED_CARD_TYPE_Q = anyOf(
            w("noncreature").thenReturn(Selector.Qualifier.negatedCardType(CardType.CREATURE)),
            w("nonartifact").thenReturn(Selector.Qualifier.negatedCardType(CardType.ARTIFACT)),
            w("nonenchantment").thenReturn(Selector.Qualifier.negatedCardType(CardType.ENCHANTMENT)),
            w("nonland").thenReturn(Selector.Qualifier.negatedCardType(CardType.LAND)),
            w("nonplaneswalker").thenReturn(Selector.Qualifier.negatedCardType(CardType.PLANESWALKER)));

    private static final Parser<Selector.Qualifier> NEGATED_SUBTYPE_Q =
            anyOf(string("non-"), string("Non-")).then(SUBTYPE).map(Selector.Qualifier::negatedSubtype);

    private static final Parser<Selector.Qualifier> STATUS_Q = anyOf(
            w("tapped").thenReturn(Selector.Qualifier.Status.TAPPED),
            w("untapped").thenReturn(Selector.Qualifier.Status.UNTAPPED),
            w("face-down").thenReturn(Selector.Qualifier.Status.FACE_DOWN),
            w("face-up").thenReturn(Selector.Qualifier.Status.FACE_UP),
            // "exiled" — zone-located in exile, used as an adjectival
            // qualifier (Pull from Eternity: "target face-up exiled
            // card").
            w("exiled").thenReturn(Selector.Qualifier.Status.EXILED),
            // Resolution-history participles — cards that were acted on
            // during the current resolution (Heed the Mists: "the milled
            // card's mana value").
            w("milled").thenReturn(Selector.Qualifier.Status.MILLED),
            w("drawn").thenReturn(Selector.Qualifier.Status.DRAWN),
            w("discarded").thenReturn(Selector.Qualifier.Status.DISCARDED),
            w("revealed").thenReturn(Selector.Qualifier.Status.REVEALED),
            // "suspended" — Venser's Diffusion: "Return target nonland
            // permanent or suspended card to its owner's hand.".
            w("suspended").thenReturn(Selector.Qualifier.Status.SUSPENDED),
            // Commander-role status (Commander-format), used on cards
            // that filter by whether a permanent is a commander
            // (Subjugate the Hobbits: "each noncommander creature").
            w("commander").thenReturn(Selector.Qualifier.Status.COMMANDER),
            w("noncommander").thenReturn(Selector.Qualifier.Status.NONCOMMANDER));

    private static final Parser<Selector.Qualifier> COMBAT_STATUS_Q = anyOf(
            // Multi-word combined forms first (longer match before shorter).
            ciWords("attacking or blocking").thenReturn(Selector.Qualifier.combatStatus("attacking or blocking")),
            // "attacking you" — directed attack marker (e.g., Blessed Reversal:
            // "for each creature attacking you").
            ciWords("attacking you").thenReturn(Selector.Qualifier.combatStatus("attacking you")),
            w("attacking").thenReturn(Selector.Qualifier.combatStatus("attacking")),
            w("blocking").thenReturn(Selector.Qualifier.combatStatus("blocking")),
            w("blocked").thenReturn(Selector.Qualifier.combatStatus("blocked")),
            w("unblocked").thenReturn(Selector.Qualifier.combatStatus("unblocked")),
            // Negated combat statuses (Alarum: "target nonattacking creature").
            w("nonattacking").thenReturn(Selector.Qualifier.combatStatus("nonattacking")),
            w("nonblocking").thenReturn(Selector.Qualifier.combatStatus("nonblocking")));

    private static final Parser<Selector.Qualifier> HISTORIC_Q = w("historic").thenReturn(Selector.Qualifier.HISTORIC);

    /// "Commander" — selects the designated commander (rule 903) in
    /// Commander-format oracle text (Bloodsworn Steward). Treated as a
    /// status qualifier since "commander" isn't a formal supertype or
    /// subtype in rule 205.
    private static final Parser<Selector.Qualifier> COMMANDER_Q =
            w("commander").thenReturn(Selector.Qualifier.Status.COMMANDER);

    /// "last" / "first" / "top" — positional qualifier (Jandor's Ring:
    /// "the last card you drew this turn"). Rendered as a status-style
    /// qualifier since these aren't formal supertypes.
    private static final Parser<Selector.Qualifier> POSITIONAL_Q = anyOf(
            w("last").thenReturn(Selector.Qualifier.Status.LAST),
            w("first").thenReturn(Selector.Qualifier.Status.FIRST),
            w("top").thenReturn(Selector.Qualifier.Status.TOP));

    private static final Parser<Selector.Qualifier> OUTLAW_Q = w("outlaw").thenReturn(Selector.Qualifier.OUTLAW);

    private static final Parser<Selector.Qualifier> NON_OUTLAW_Q =
            string("non-").then(word("outlaw")).thenReturn(Selector.Qualifier.NEGATED_OUTLAW);

    private static final Parser<Selector.Qualifier> TOKEN_Q = w("token").thenReturn(Selector.Qualifier.IS_TOKEN);

    private static final Parser<Selector.Qualifier> NONTOKEN_Q = w("nontoken").thenReturn(Selector.Qualifier.NON_TOKEN);

    private static final Parser<Selector.Qualifier> OTHER_Q = w("other").thenReturn(Selector.Qualifier.OTHER);

    private static final Parser<Selector.Qualifier> ENCHANTED_Q =
            w("enchanted").thenReturn(Selector.Qualifier.Enchanted.ENCHANTED);

    /// "kicked" — spell cast with its kicker cost (rule 702.33). Used
    /// as a selector qualifier on spell triggers (Merfolk Falconer).
    private static final Parser<Selector.Qualifier> KICKED_Q =
            w("kicked").thenReturn(Selector.Qualifier.combatStatus("kicked"));

    private static final Parser<Selector.Qualifier> EQUIPPED_Q =
            w("equipped").thenReturn(Selector.Qualifier.Equipped.EQUIPPED);

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
            HISTORIC_Q,
            COMMANDER_Q,
            POSITIONAL_Q,
            OUTLAW_Q,
            NONTOKEN_Q,
            TOKEN_Q,
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
    private static final Parser<List<Selector.Qualifier>> QUALIFIER_LIST =
            QUALIFIER.optionallyFollowedBy(",").atLeastOnce();

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
            // "you" / "they" / "an" bound "with flashback" so a trailing
            // controller-clause ("you own", "you control", "they own", "an
            // opponent controls") remains available for the outer Selector
            // (Runic Repetition: "target exiled card with flashback you
            // own").
            "you",
            "they",
            "an");

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
            word("devoid").thenReturn(Ability.StaticKeyword.DEVOID));

    /// Token inside a free-text with-clause predicate — plain words plus
    /// "+1/+1" / "-1/-1" counter markers (Herald of Secret Streams) and
    /// possessive apostrophes (Wandering Wolf: "Creatures with power
    /// less than this creature's power").
    private static final Parser<String> WITH_PREDICATE_TOKEN = anyOf(
            consecutive(CharacterSet.charsIn("[0-9+/-]"), "with-predicate pt marker"),
            consecutive(CharacterSet.charsIn("[A-Za-z'-]"), "with-predicate word"));

    private static final Parser<Selector.WithClause> WITH_CLAUSE = sequence(
            anyOf(w("with").thenReturn(false), w("without").thenReturn(true)),
            // Try a structural keyword-ability reference first so "with flying"
            // becomes a {@link WithClause.HasAbility} holding the Ability;
            // falls back to a free-text predicate for phrases the grammar
            // hasn't structured yet (e.g., "with flashback", "with cycling",
            // "with a +1/+1 counter on it").
            Parser.<Selector.WithClause>anyOf(
                    WITH_KEYWORD_NAME.map(
                            ability -> (Selector.WithClause) new Selector.WithClause.HasAbility(false, ability)),
                    WITH_PREDICATE_TOKEN
                            .suchThat(w -> !WITH_STOP_WORDS.contains(w.toLowerCase()), "with-clause word")
                            .atLeastOnce()
                            .map(words -> (Selector.WithClause)
                                    new Selector.WithClause.HasPredicate(false, String.join(" ", words)))),
            (negated, clause) -> clause instanceof Selector.WithClause.HasAbility ha
                    ? new Selector.WithClause.HasAbility(negated, ha.ability())
                    : new Selector.WithClause.HasPredicate(
                            negated, ((Selector.WithClause.HasPredicate) clause).predicate()));

    // ── Or-alternative and TYPE_EXPRESSION (depend on QUALIFIER and WITH_CLAUSE) ─

    /// One disjunct in an [Selector.TypeExpression.Or] — optional
    /// leading qualifiers, a [#TYPE_GROUP], and an optional
    /// trailing [#WITH_CLAUSE]. Each alternative owns its own
    /// qualifiers and with-clauses so "enchanted creature or enchantment
    /// creature" and "Spirit, creature with disturb, or enchantment"
    /// round-trip with branch-local context.
    private static final Parser<Selector.TypeExpression.Or.Alternative> OR_ALTERNATIVE = anyOf(
                    sequence(QUALIFIER_LIST, TYPE_GROUP, Selector.TypeExpression.Or.Alternative::new),
                    TYPE_GROUP.map(Selector.TypeExpression.Or.Alternative::new))
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

    public static final Parser<Selector.TypeExpression> TYPE_EXPRESSION = anyOf(
            OR_TYPE_WITH_OBJECT,
            AND_TYPE_WITH_OBJECT,
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
            ciWords("the battlefield"),
            ciWords("their owners' hands"),
            ciWords("its owner's hand"),
            ciWords("their owner's hand"),
            ciWords("your hand"),
            ciWords("their hand"));

    /// One token of a [#THAT_CLAUSE] predicate. Either:
    /// - "to" when it isn't introducing a destination (stays in the
    ///   predicate; e.g., Reciprocate).
    /// - any other contraction-word that isn't a verb stop word.
    /// Order matters: the "to" arm is tried first so the lookahead can
    /// short-circuit before the catch-all matches "to" via the second
    /// arm.
    private static final Parser<String> THAT_CLAUSE_WORD = anyOf(
            w("to").notFollowedBy(DESTINATION_AFTER_TO, "to-destination"),
            CONTRACTION_WORD.suchThat(
                    w -> !THAT_STOP_WORDS.contains(w.toLowerCase()) && !w.equalsIgnoreCase("to"), "that-clause word"));

    /// "that [predicate]" — relative clause. Stops at the containing
    /// effect's verb (see [#THAT_STOP_WORDS]) or before a
    /// "to <destination>" tail (see [#DESTINATION_AFTER_TO]).
    private static final Parser<Selector.ThatClause> THAT_CLAUSE = w("that")
            .then(THAT_CLAUSE_WORD.atLeastOnce().map(words -> String.join(" ", words)))
            .map(Selector.ThatClause::new);

    // ── Controller clause ──────────────────────────────────────────────

    private static Selector.ControllerClause controls(Selector.ControllerClause.Who who, boolean negated) {
        return new Selector.ControllerClause.Controls(who, negated);
    }

    private static final Parser<Selector.ControllerClause> CONTROLLER_CLAUSE = anyOf(
            ciWords("you don't control").thenReturn(controls(Selector.ControllerClause.Who.YOU, true)),
            ciWords("you control").thenReturn(controls(Selector.ControllerClause.Who.YOU, false)),
            ciWords("you cast").thenReturn((Selector.ControllerClause)
                    new Selector.ControllerClause.Casts(Selector.ControllerClause.Who.YOU)),
            // "you've cast" — past-tense contraction (e.g., Multani's
            // Presence: "a spell you've cast"). Matched as word + literal
            // "'ve" + word because Parser.word() doesn't span apostrophes.
            w("you").then(string("'ve")).then(word("cast")).thenReturn((Selector.ControllerClause)
                    new Selector.ControllerClause.Casts(Selector.ControllerClause.Who.YOU)),
            ciWords("your team controls").thenReturn(controls(Selector.ControllerClause.Who.YOUR_TEAM, false)),
            ciWords("an opponent controls").thenReturn(controls(Selector.ControllerClause.Who.AN_OPPONENT, false)),
            ciWords("each opponent controls").thenReturn(controls(Selector.ControllerClause.Who.EACH_OPPONENT, false)),
            ciWords("your opponents control").thenReturn(controls(Selector.ControllerClause.Who.YOUR_OPPONENTS, false)),
            ciWords("target player controls").thenReturn(controls(Selector.ControllerClause.Who.TARGET_PLAYER, false)),
            ciWords("target opponent controls")
                    .thenReturn(controls(Selector.ControllerClause.Who.TARGET_OPPONENT, false)),
            ciWords("enchanted player controls")
                    .thenReturn(controls(Selector.ControllerClause.Who.ENCHANTED_PLAYER, false)),
            ciWords("its controller controls")
                    .thenReturn(controls(Selector.ControllerClause.Who.ITS_CONTROLLER, false)),
            ciWords("they control").thenReturn(controls(Selector.ControllerClause.Who.THEY, false)),
            ciWords("target player owns").thenReturn((Selector.ControllerClause)
                    new Selector.ControllerClause.Owns(Selector.ControllerClause.Who.TARGET_PLAYER)),
            ciWords("you own").thenReturn((Selector.ControllerClause)
                    new Selector.ControllerClause.Owns(Selector.ControllerClause.Who.YOU)),
            ciWords("an opponent owns").thenReturn((Selector.ControllerClause)
                    new Selector.ControllerClause.Owns(Selector.ControllerClause.Who.AN_OPPONENT)),
            ciWords("they own").thenReturn((Selector.ControllerClause)
                    new Selector.ControllerClause.Owns(Selector.ControllerClause.Who.THEY)));

    // ── Selector ───────────────────────────────────────────────────────

    /// Hoists the `target` qualifier out of per-branch qualifiers
    /// up to the outer Selector. Oracle text typically names `target`
    /// once (on the first alternative) with the semantic that it applies
    /// to the whole disjunction; this normalizes that reading by moving
    /// TARGET onto [Selector]'s shared qualifier list.
    private static Selector hoistTarget(Selector.Quantifier quant, Selector.TypeExpression type) {
        if (type instanceof Selector.TypeExpression.Or(var alts) && !alts.isEmpty()) {
            var first = alts.getFirst();
            if (first.qualifiers().contains(Selector.Qualifier.TARGET)) {
                var stripped = first.qualifiers().stream()
                        .filter(q -> q != Selector.Qualifier.TARGET)
                        .toList();
                var newAlts = new ArrayList<>(alts);
                newAlts.set(0, new Selector.TypeExpression.Or.Alternative(stripped, first.type()));
                return new Selector(
                        quant,
                        List.of(Selector.Qualifier.TARGET),
                        new Selector.TypeExpression.Or(List.copyOf(newAlts)));
            }
        }
        return new Selector(quant, List.of(), type);
    }

    /// A single-branch selector body — the non-Or case where one
    /// [#OR_ALTERNATIVE] describes the selector tail. Qualifiers and
    /// with-clauses become the outer [Selector]'s (not shared
    /// across siblings because there are none).
    private static Selector flatFromAlt(Selector.Quantifier quant, Selector.TypeExpression.Or.Alternative alt) {
        return new Selector(quant, alt.qualifiers(), alt.type(), alt.withClauses(), null);
    }

    /// Multi-alternative type expression: [#OR_TYPE] / [#AND_TYPE] /
    /// [#AND_OR_TYPE] all yield a
    /// [Selector.TypeExpression.Or] and are interchangeable at the
    /// selector-body level. AND_OR is tried first because its literal
    /// "and/or" is a longer match than "and" or "or" alone.
    private static final Parser<Selector.TypeExpression> MULTI_ALT_TYPE = anyOf(AND_OR_TYPE, OR_TYPE, AND_TYPE);

    /// Build selector from parts: quantifier? (or-alternative | or-type)
    /// withClause* controllerClause?. The "or" case produces a
    /// [Selector.TypeExpression.Or] with per-branch qualifiers; the
    /// non-Or case flattens the alternative's qualifiers onto the outer
    /// Selector. `target` is hoisted to the shared Selector
    /// qualifier list (see [#hoistTarget]).
    private static final Parser<Selector> BASE_SELECTOR_OR =
            sequence(QUANTIFIER, MULTI_ALT_TYPE, SelectorParsers::hoistTarget);

    private static final Parser<Selector> BASE_SELECTOR_ALT =
            sequence(QUANTIFIER, OR_ALTERNATIVE, SelectorParsers::flatFromAlt);

    private static final Parser<Selector> BARE_SELECTOR_OR =
            MULTI_ALT_TYPE.map(or -> hoistTarget(Selector.Quantifier.one(), or));

    private static final Parser<Selector> BARE_SELECTOR_ALT =
            OR_ALTERNATIVE.map(alt -> flatFromAlt(Selector.Quantifier.one(), alt));

    private static final Parser<Selector> CORE_SELECTOR = anyOf(
            BASE_SELECTOR_OR, // multi-branch must precede single-alt
            BASE_SELECTOR_ALT,
            BARE_SELECTOR_OR,
            BARE_SELECTOR_ALT);

    /// "in [possessive] [zone]" or "in [plural-zone]" — trailing zone scope
    /// on a selector ("cards in your hand", "cards in graveyards").
    private static final Parser<Zone.Named> ZONE_CLAUSE = anyOf(
            // Multi-word possessives first so longer matches win.
            sequence(w("in").then(anyOf(words("an opponent's"), words("each opponent's"))), ZONE_NAME, Zone.Named::new),
            sequence(w("in").then(anyWord("your", "their", "its", "a", "any")), ZONE_NAME, Zone.Named::new),
            w("in").then(anyOf(word("all").then(PLURAL_ZONE_NAME), PLURAL_ZONE_NAME))
                    .map(z -> new Zone.Named(null, z)));

    /// "played by [player]" — cast-history participle (e.g., Uphill Battle:
    /// "Creatures played by your opponents enter tapped."). Captures the
    /// player phrase as free text bounded by [#WITH_STOP_WORDS] to
    /// avoid pulling in the trailing effect verb.
    private static final Parser<Selector.ThatClause> PLAYED_BY = ciWords("played by")
            .then(CONTRACTION_WORD
                    .suchThat(w -> !WITH_STOP_WORDS.contains(w.toLowerCase()), "played-by word")
                    .atLeastOnce()
                    .map(words -> String.join(" ", words)))
            .map(s -> new Selector.ThatClause("played by " + s));

    /// "of the [card type | creature type | color] of [owner]'s choice"
    /// — selector modifier naming a category chosen by the player
    /// (Extinction: "Destroy all creatures of the creature type of your
    /// choice."). The chosen dimension is captured as free text.
    private static final Parser<Selector.ThatClause> OF_CHOICE_CATEGORY = anyOf(
            ciWords("of the")
                    .then(anyOf(
                            ciWords("creature type"),
                            ciWords("card type"),
                            ciWords("color"),
                            ciWords("land type"),
                            ciWords("subtype")))
                    .followedBy(phrase("of [your|their|its|an|any] choice"))
                    .map(category -> new Selector.ThatClause("of the " + category + " of <owner>'s choice")),
            // "of [poss] choice" — direct selector-level chooser (Pay No
            // Heed: "a source of your choice"; Clip Wings: "a creature of
            // their choice").
            ciWords("of")
                    .then(anyWord("your", "their", "its", "an", "any"))
                    .followedBy(word("choice"))
                    .map(poss -> new Selector.ThatClause("of " + poss + " choice")));

    /// Forward-declared rule tying back to [#SELECTOR] so participles
    /// like [#ATTACHED_TO] can nest a full selector inside themselves
    /// (breaking the static-init cycle between the outer SELECTOR and its
    /// inner participle clauses).
    private static final Parser.Rule<Selector> SELECTOR_RULE = new Parser.Rule<>();

    /// "attached to [selector|pronoun]" — attachment participle (Devout
    /// Harpist: "Destroy target Aura attached to a creature."; Miracle
    /// Worker: "attached to a creature you control"; Graceblade Artisan:
    /// "for each Aura attached to it."). Accepts a bare pronoun ("it" /
    /// "them" / "itself") in addition to a full [#SELECTOR] so the
    /// pronoun-referenced form doesn't fall through to SELECTOR and leave
    /// the pronoun unconsumed.
    private static final Parser<Selector.ThatClause> ATTACHED_TO = ciWords("attached to")
            .then(anyOf(anyWord("it", "them", "itself"), SELECTOR_RULE.map(Object::toString)))
            .map(s -> new Selector.ThatClause("attached to " + s));

    /// "cast from [zone]" — origin-zone participle on spells (e.g.,
    /// Laquatus's Disdain: "Counter target spell cast from a graveyard.").
    /// The zone is captured as `[article] <zone-name>`.
    private static final Parser<Selector.ThatClause> CAST_FROM_PARTICIPLE = sequence(
            ciWords("cast from").then(anyWord("a", "an", "the", "your", "their", "its")),
            ZONE_NAME,
            (poss, zone) -> new Selector.ThatClause(
                    "cast from " + poss + " " + zone.name().toLowerCase()));

    /// "blocking [subject]" — directed-block participle (e.g., Knight of
    /// Dusk: "Destroy target creature blocking this creature."). Captures
    /// the target as free text bounded by [#WITH_STOP_WORDS] so we
    /// avoid a static-init cycle with [SubjectParsers]. Tried before
    /// the bare "blocking" participle so the longer match wins.
    private static final Parser<Selector.ThatClause> BLOCKING_SUBJECT = w("blocking")
            .then(CONTRACTION_WORD
                    .suchThat(w -> !WITH_STOP_WORDS.contains(w.toLowerCase()), "blocking-subject word")
                    .atLeastOnce()
                    .map(words -> String.join(" ", words)))
            .map(s -> new Selector.ThatClause("blocking " + s));

    /// Participial suffix (`attacking you`, `blocking`, `attacking or
    /// blocking`, `played by X`, `attached to X`) without an explicit
    /// `that is …`. Oracle text attaches these directly to a type:
    /// `creature attacking you` = `creature that is attacking you`.
    /// Rendered into a [Selector.ThatClause].
    private static final Parser<Selector.ThatClause> PARTICIPIAL_CLAUSE = anyOf(
            PLAYED_BY,
            ATTACHED_TO,
            CAST_FROM_PARTICIPLE,
            OF_CHOICE_CATEGORY,
            ciWords("attacking you").map(Selector.ThatClause::new),
            ciWords("attacking or blocking").map(Selector.ThatClause::new),
            w("attacking").map(Selector.ThatClause::new),
            BLOCKING_SUBJECT, // must precede the bare "blocking"
            w("blocking").map(Selector.ThatClause::new),
            w("blocked").map(Selector.ThatClause::new),
            w("unblocked").map(Selector.ThatClause::new),
            // "dealt damage this turn" / "dealt damage" — damage-history
            // participle (Inflame: "each creature dealt damage this
            // turn.").
            ciWords("dealt damage this turn").map(Selector.ThatClause::new),
            ciWords("dealt damage").map(Selector.ThatClause::new),
            // "named X" — name-equality clause (Powerstone Shard: "each
            // artifact you control named Powerstone Shard"). Self-reference
            // substitution has already replaced the card's own name with
            // "~", which we accept as an alternate form.
            ciWords("named")
                    .then(anyOf(
                            string("~"),
                            word().suchThat(s -> !s.isEmpty() && Character.isUpperCase(s.charAt(0)), "named-card word")
                                    .atLeastOnce()
                                    .map(ws -> String.join(" ", ws))))
                    .map(name -> new Selector.ThatClause("named " + name)),
            // "you drew this turn" — draw-history participle (Jandor's
            // Ring: "the last card you drew this turn"). Currently the
            // clause text is captured verbatim; the controller can be
            // tightened later if needed.
            ciWords("you drew this turn").map(Selector.ThatClause::new));

    /// "except for <type>" — trailing exclusion clause (Slash the Ranks:
    /// "Destroy all creatures and planeswalkers except for commanders.").
    /// Stored as a negated [Selector.WithClause] so the existing
    /// with-clause channel carries both inclusion and exclusion filters.
    private static final Parser<Selector.WithClause> EXCEPT_CLAUSE = ciWords("except for")
            .then(word().suchThat(w -> !WITH_STOP_WORDS.contains(w.toLowerCase()), "except-clause word")
                    .atLeastOnce()
                    .map(words -> String.join(" ", words)))
            .map(text -> (Selector.WithClause) new Selector.WithClause.HasPredicate(true, "except for " + text));

    public static final Parser<Selector> SELECTOR = CORE_SELECTOR
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
            .optionallyFollowedBy(ZONE_CLAUSE, Selector::withZone);

    static {
        SELECTOR_RULE.definedAs(SELECTOR);
    }
}
