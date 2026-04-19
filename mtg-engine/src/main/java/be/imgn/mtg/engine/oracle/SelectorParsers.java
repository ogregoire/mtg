package be.imgn.mtg.engine.oracle;

import static be.imgn.mtg.engine.oracle.Words.anyCiWord;
import static be.imgn.mtg.engine.oracle.Words.ciWords;
import static be.imgn.mtg.engine.oracle.Words.w;
import static com.google.common.labs.parse.Parser.anyOf;
import static com.google.common.labs.parse.Parser.consecutive;
import static com.google.common.labs.parse.Parser.digits;
import static com.google.common.labs.parse.Parser.or;
import static com.google.common.labs.parse.Parser.sequence;
import static com.google.common.labs.parse.Parser.string;
import static com.google.common.labs.parse.Parser.word;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
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

    /// An integer written either as digits ({@code 3}) or an English word
    /// number ({@code three}). Prefer this when oracle text accepts both.
    public static final Parser<Integer> NUMBER = anyOf(WORD_NUMBER, INTEGER);

    // ── Amount ─────────────────────────────────────────────────────────

    /// A single-term amount — the atom before the optional `plus` suffix.
    private static final Parser<Amount> ATOMIC_AMOUNT = anyOf(
            word("X").thenReturn(Amount.variable()),
            w("that").then(anyCiWord("much", "many")).map(w -> Amount.reference("that " + w)),
            WORD_NUMBER.map(Amount::exact),
            INTEGER.map(Amount::exact),
            anyCiWord("a", "an").thenReturn(Amount.exact(1)));

    /// Amount expression, optionally followed by `plus <atom>` for arithmetic
    /// like "X plus 3".
    public static final Parser<Amount> AMOUNT =
            ATOMIC_AMOUNT.optionallyFollowedBy(w("plus").then(ATOMIC_AMOUNT), Amount.Plus::new);

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
            w("kindred").thenReturn(CardType.KINDRED));

    public static final Parser<GameObjectType> GAME_OBJECT_TYPE = anyOf(
            anyCiWord("permanents", "permanent").thenReturn(GameObjectType.PERMANENT),
            anyCiWord("spells", "spell").thenReturn(GameObjectType.SPELL),
            anyCiWord("cards", "card").thenReturn(GameObjectType.CARD),
            anyCiWord("tokens", "token").thenReturn(GameObjectType.TOKEN),
            anyCiWord("sources", "source").thenReturn(GameObjectType.SOURCE),
            anyCiWord("abilities", "ability").thenReturn(GameObjectType.ABILITY));

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

    /// Builds a parser that accepts every {@link Subtype#texts() form} of the
    /// given subtype enum, returning the canonical enum constant. Longest
    /// forms are tried first so plural "Goblins" wins over singular "Goblin".
    private static <E extends Enum<E> & Subtype> Parser<Subtype> subtypeForms(E[] values) {
        record Form(String match, Subtype canonical) {}
        var seen = new LinkedHashSet<String>();
        var forms = new ArrayList<Form>();
        for (var e : values) {
            for (var form : e.texts()) {
                if (seen.add(form)) forms.add(new Form(form, e));
            }
        }
        forms.sort(Comparator.comparingInt((Form f) -> f.match().length()).reversed());
        return forms.stream()
                .<Parser<Subtype>>map(f -> word(f.match()).thenReturn(f.canonical()))
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

    /// Subtype parser that returns the canonical singular text — used by
    /// places still carrying subtypes as strings ({@link Selector.SingleType
    /// .OfSubtype}, {@code NegatedSubtype}, token parsing, {@code
    /// SetSubtype}).
    public static final Parser<String> SUBTYPE_NAME = SUBTYPE.map(Subtype::text);

    private static final Parser<Selector.SingleType> SUBTYPE_SINGLE = SUBTYPE_NAME.map(Selector.SingleType::ofSubtype);

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
    /// spell", "creature or sorcery spell". Must precede {@link #OR_TYPE} so
    /// the trailing game-object is not consumed by a {@code COMPOUND_TYPE}.
    private static final Parser<Selector.TypeExpression> OR_TYPE_WITH_OBJECT = sequence(
            REFINEMENT.followedBy(w("or")),
            REFINEMENT,
            GAME_OBJECT_TYPE,
            (a, b, obj) -> Selector.TypeExpression.or(List.of(combine(a, obj), combine(b, obj))));

    /// "[X] and [Y] [game-object]" — same shape as {@link #OR_TYPE_WITH_OBJECT}
    /// with `and` instead of `or` (e.g., Arcane Melee: "Instant and sorcery
    /// spells cost {2} less to cast."). Modelled as an Or at the type level
    /// since both types qualify matching game-objects.
    private static final Parser<Selector.TypeExpression> AND_TYPE_WITH_OBJECT = sequence(
            REFINEMENT.followedBy(w("and")),
            REFINEMENT,
            GAME_OBJECT_TYPE,
            (a, b, obj) -> Selector.TypeExpression.or(List.of(combine(a, obj), combine(b, obj))));

    /// "X or Y" / "X, Y, or Z" / "W, X, Y, or Z" — Oxford-comma or-list of
    /// types (e.g., Reclaiming Vines: "artifact, enchantment, or land").
    private static final Parser<Selector.TypeExpression> OR_TYPE = MtgParsers.orList(SINGLE_TYPE)
            .suchThat(l -> l.size() >= 2, "or-list of types")
            .map(Selector.TypeExpression::or);

    /// "X and Y" (and "X, Y, and Z") — union of types. Modelled as an Or
    /// expression since both denote "permanents matching any listed type".
    private static final Parser<Selector.TypeExpression> AND_TYPE = MtgParsers.andList(SINGLE_TYPE)
            .suchThat(l -> l.size() >= 2, "and-list of types")
            .map(Selector.TypeExpression::or);

    private static final Parser<Selector.TypeExpression> COMPOUND_TYPE = SINGLE_TYPE
            .atLeastOnce()
            .suchThat(list -> list.size() >= 2, "compound type")
            .map(Selector.TypeExpression::compound);

    private static final Parser<Selector.TypeExpression> SINGLE_WRAP = SINGLE_TYPE.map(Selector.TypeExpression::single);

    public static final Parser<Selector.TypeExpression> TYPE_EXPRESSION =
            anyOf(OR_TYPE_WITH_OBJECT, AND_TYPE_WITH_OBJECT, OR_TYPE, AND_TYPE, COMPOUND_TYPE, SINGLE_WRAP);

    // ── Quantifier ─────────────────────────────────────────────────────

    public static final Parser<Selector.Quantifier> QUANTIFIER = anyOf(
            w("all").thenReturn(Selector.Quantifier.all()),
            w("each").thenReturn(Selector.Quantifier.each()),
            w("every").thenReturn(Selector.Quantifier.every()),
            w("another").thenReturn(Selector.Quantifier.another()),
            w("other").thenReturn(Selector.Quantifier.other()),
            w("the").thenReturn(Selector.Quantifier.the()),
            word("X").thenReturn(Selector.Quantifier.variable()),
            ciWords("up to").then(anyOf(WORD_NUMBER, INTEGER)).map(Selector.Quantifier::upTo),
            ciWords("any number of").thenReturn(Selector.Quantifier.anyNumber()),
            // "N or M" — inclusive range. Tried before bare N so the trailing
            // " or M" isn't left for a downstream selector-level "or".
            sequence(
                    anyOf(WORD_NUMBER, INTEGER), w("or").then(anyOf(WORD_NUMBER, INTEGER)), Selector.Quantifier::range),
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

    /// "[color] or [color] …" — single or Oxford list of color filters.
    /// Emits {@code Color} for a single filter, {@code Colors} for a list.
    private static final Parser<Selector.Qualifier> COLOR_Q = MtgParsers.orList(COLOR_FILTER)
            .map(filters -> filters.size() == 1
                    ? Selector.Qualifier.color(filters.getFirst())
                    : new Selector.Qualifier.Colors(filters));

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
            anyOf(string("non-"), string("Non-")).then(SUBTYPE_NAME).map(Selector.Qualifier::negatedSubtype);

    private static final Parser<Selector.Qualifier> STATUS_Q = anyOf(
            w("tapped").thenReturn(Selector.Qualifier.status("tapped")),
            w("untapped").thenReturn(Selector.Qualifier.status("untapped")),
            w("face-down").thenReturn(Selector.Qualifier.status("face-down")),
            w("face-up").thenReturn(Selector.Qualifier.status("face-up")));

    private static final Parser<Selector.Qualifier> COMBAT_STATUS_Q = anyOf(
            // Multi-word combined forms first (longer match before shorter).
            ciWords("attacking or blocking").thenReturn(Selector.Qualifier.combatStatus("attacking or blocking")),
            // "attacking you" — directed attack marker (e.g., Blessed Reversal:
            // "for each creature attacking you").
            ciWords("attacking you").thenReturn(Selector.Qualifier.combatStatus("attacking you")),
            w("attacking").thenReturn(Selector.Qualifier.combatStatus("attacking")),
            w("blocking").thenReturn(Selector.Qualifier.combatStatus("blocking")),
            w("blocked").thenReturn(Selector.Qualifier.combatStatus("blocked")),
            w("unblocked").thenReturn(Selector.Qualifier.combatStatus("unblocked")));

    private static final Parser<Selector.Qualifier> HISTORIC_Q = w("historic").thenReturn(Selector.Qualifier.HISTORIC);

    private static final Parser<Selector.Qualifier> OUTLAW_Q = w("outlaw").thenReturn(Selector.Qualifier.OUTLAW);

    private static final Parser<Selector.Qualifier> NON_OUTLAW_Q =
            string("non-").then(w("outlaw")).thenReturn(Selector.Qualifier.NEGATED_OUTLAW);

    private static final Parser<Selector.Qualifier> TOKEN_Q = w("token").thenReturn(Selector.Qualifier.IS_TOKEN);

    private static final Parser<Selector.Qualifier> NONTOKEN_Q = w("nontoken").thenReturn(Selector.Qualifier.NON_TOKEN);

    private static final Parser<Selector.Qualifier> OTHER_Q = w("other").thenReturn(Selector.Qualifier.OTHER);

    private static final Parser<Selector.Qualifier> ENCHANTED_Q =
            w("enchanted").thenReturn(Selector.Qualifier.Enchanted.ENCHANTED);

    private static final Parser<Selector.Qualifier> EQUIPPED_Q =
            w("equipped").thenReturn(Selector.Qualifier.Equipped.EQUIPPED);

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
            OUTLAW_Q,
            NONTOKEN_Q,
            TOKEN_Q,
            OTHER_Q,
            ENCHANTED_Q,
            EQUIPPED_Q);

    // ── With clause ────────────────────────────────────────────────────

    /// Words that signal the end of the selector and the start of an outer
    /// effect clause — used to bound the WITH_CLAUSE predicate so it doesn't
    /// greedily consume "get" / "gets" / "can't" / etc. after a "with" clause.
    private static final Set<String> WITH_STOP_WORDS = Set.of(
            "get", "gets", "have", "has", "deal", "deals", "enter", "enters", "are", "is", "ca", "can", "lose", "loses",
            "gain", "gains", "attack", "attacks", "block", "blocks");

    private static final Parser<Selector.WithClause> WITH_CLAUSE = sequence(
            anyOf(w("with").thenReturn(false), w("without").thenReturn(true)),
            word().suchThat(w -> !WITH_STOP_WORDS.contains(w.toLowerCase()), "with-clause word")
                    .atLeastOnce()
                    .map(words -> String.join(" ", words)),
            Selector.WithClause::new);

    /// A word-with-contraction token (e.g., "isn't", "doesn't"). Broader than
    /// {@link Parser#word()} so relative clauses can include English
    /// contractions like "that isn't all colors".
    private static final Parser<String> CONTRACTION_WORD = consecutive(CharacterSet.charsIn("[A-Za-z0-9'-]"), "word");

    /// Stop-words for the that-clause predicate — narrower than
    /// {@link #WITH_STOP_WORDS} because `that are …` / `that is …` are
    /// valid opening forms (e.g., Song of Serenity: "creatures that are
    /// enchanted …"), so "are"/"is" must be allowed inside the predicate.
    /// Contractions like `can't` are captured as a single
    /// {@link #CONTRACTION_WORD} token and need explicit entries here.
    private static final Set<String> THAT_STOP_WORDS = Set.of(
            "get", "gets", "have", "has", "deal", "deals", "enter", "enters", "can", "can't", "lose", "loses", "gain",
            "gains", "attack", "attacks", "block", "blocks", "must");

    /// "that [predicate]" — relative clause. Stops at the containing
    /// effect's verb (see {@link #THAT_STOP_WORDS}).
    private static final Parser<Selector.ThatClause> THAT_CLAUSE = w("that")
            .then(CONTRACTION_WORD
                    .suchThat(w -> !THAT_STOP_WORDS.contains(w.toLowerCase()), "that-clause word")
                    .atLeastOnce()
                    .map(words -> String.join(" ", words)))
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
            ciWords("your team controls").thenReturn(controls(Selector.ControllerClause.Who.YOUR_TEAM, false)),
            ciWords("an opponent controls").thenReturn(controls(Selector.ControllerClause.Who.AN_OPPONENT, false)),
            ciWords("each opponent controls").thenReturn(controls(Selector.ControllerClause.Who.EACH_OPPONENT, false)),
            ciWords("your opponents control").thenReturn(controls(Selector.ControllerClause.Who.YOUR_OPPONENTS, false)),
            ciWords("target player controls").thenReturn(controls(Selector.ControllerClause.Who.TARGET_PLAYER, false)),
            ciWords("target opponent controls")
                    .thenReturn(controls(Selector.ControllerClause.Who.TARGET_OPPONENT, false)),
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

    // Build selector from parts: quantifier? qualifier* typeExpression withClause* controllerClause?
    // Using chained optionallyFollowedBy for optional suffixes.
    private static final Parser<Selector> BASE_SELECTOR = sequence(QUANTIFIER, TYPE_EXPRESSION, Selector::new);

    private static final Parser<Selector> QUALIFIED_SELECTOR =
            sequence(QUANTIFIER, QUALIFIER.atLeastOnce(), TYPE_EXPRESSION, Selector::new);

    /// Bare type expression with no quantifier: "creatures", "permanents".
    private static final Parser<Selector> BARE_SELECTOR = TYPE_EXPRESSION.map(Selector::new);

    /// Qualifiers with no explicit quantifier: "target creature", "red permanent".
    private static final Parser<Selector> QUALIFIERS_ONLY_SELECTOR =
            sequence(QUALIFIER.atLeastOnce(), TYPE_EXPRESSION, Selector::new);

    private static final Parser<Selector> CORE_SELECTOR =
            anyOf(QUALIFIED_SELECTOR, BASE_SELECTOR, QUALIFIERS_ONLY_SELECTOR, BARE_SELECTOR);

    /// "in [possessive] [zone]" or "in [plural-zone]" — trailing zone scope
    /// on a selector ("cards in your hand", "cards in graveyards").
    private static final Parser<Zone.Named> ZONE_CLAUSE = anyOf(
            sequence(w("in").then(anyCiWord("your", "their", "its", "a", "any")), ZONE_NAME, Zone.Named::new),
            w("in").then(PLURAL_ZONE_NAME).map(z -> new Zone.Named(null, z)));

    /// "played by [player]" — cast-history participle (e.g., Uphill Battle:
    /// "Creatures played by your opponents enter tapped."). Captures the
    /// player phrase as free text bounded by {@link #WITH_STOP_WORDS} to
    /// avoid pulling in the trailing effect verb.
    private static final Parser<Selector.ThatClause> PLAYED_BY = ciWords("played by")
            .then(CONTRACTION_WORD
                    .suchThat(w -> !WITH_STOP_WORDS.contains(w.toLowerCase()), "played-by word")
                    .atLeastOnce()
                    .map(words -> String.join(" ", words)))
            .map(s -> new Selector.ThatClause("played by " + s));

    /// "attached to [subject]" — attachment participle (e.g., Devout
    /// Harpist: "Destroy target Aura attached to a creature.").
    private static final Parser<Selector.ThatClause> ATTACHED_TO = ciWords("attached to")
            .then(CONTRACTION_WORD
                    .suchThat(w -> !WITH_STOP_WORDS.contains(w.toLowerCase()), "attached-to word")
                    .atLeastOnce()
                    .map(words -> String.join(" ", words)))
            .map(s -> new Selector.ThatClause("attached to " + s));

    /// "blocking [subject]" — directed-block participle (e.g., Knight of
    /// Dusk: "Destroy target creature blocking this creature."). Captures
    /// the target as free text bounded by {@link #WITH_STOP_WORDS} so we
    /// avoid a static-init cycle with {@link SubjectParsers}. Tried before
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
    /// Rendered into a {@link Selector.ThatClause}.
    private static final Parser<Selector.ThatClause> PARTICIPIAL_CLAUSE = anyOf(
            PLAYED_BY,
            ATTACHED_TO,
            ciWords("attacking you").map(Selector.ThatClause::new),
            ciWords("attacking or blocking").map(Selector.ThatClause::new),
            w("attacking").map(Selector.ThatClause::new),
            BLOCKING_SUBJECT, // must precede the bare "blocking"
            w("blocking").map(Selector.ThatClause::new),
            w("blocked").map(Selector.ThatClause::new),
            w("unblocked").map(Selector.ThatClause::new));

    public static final Parser<Selector> SELECTOR = CORE_SELECTOR
            .optionallyFollowedBy(CONTROLLER_CLAUSE, Selector::withController)
            .optionallyFollowedBy(WITH_CLAUSE, Selector::withWithClause)
            .optionallyFollowedBy(THAT_CLAUSE, Selector::withThatClause)
            .optionallyFollowedBy(PARTICIPIAL_CLAUSE, Selector::withThatClause)
            .optionallyFollowedBy(ZONE_CLAUSE, Selector::withZone);
}
