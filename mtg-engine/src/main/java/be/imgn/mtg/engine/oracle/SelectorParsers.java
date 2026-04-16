package be.imgn.mtg.engine.oracle;

import static be.imgn.mtg.engine.oracle.Words.anyCiWord;
import static be.imgn.mtg.engine.oracle.Words.ciWords;
import static be.imgn.mtg.engine.oracle.Words.w;
import static com.google.common.labs.parse.Parser.anyOf;
import static com.google.common.labs.parse.Parser.digits;
import static com.google.common.labs.parse.Parser.sequence;
import static com.google.common.labs.parse.Parser.string;
import static com.google.common.labs.parse.Parser.word;

import java.util.List;

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

    // ── Amount ─────────────────────────────────────────────────────────

    public static final Parser<Amount> AMOUNT = anyOf(
            word("X").thenReturn(Amount.variable()),
            w("that").then(anyCiWord("much", "many")).map(w -> Amount.reference("that " + w)),
            WORD_NUMBER.map(Amount::exact),
            INTEGER.map(Amount::exact),
            anyCiWord("a", "an").thenReturn(Amount.exact(1)));

    // ── Enums ──────────────────────────────────────────────────────────

    public static final Parser<Color> COLOR = anyOf(
            w("white").thenReturn(Color.WHITE),
            w("blue").thenReturn(Color.BLUE),
            w("black").thenReturn(Color.BLACK),
            w("red").thenReturn(Color.RED),
            w("green").thenReturn(Color.GREEN));

    public static final Parser<CardType> CARD_TYPE = anyOf(
            w("creature").thenReturn(CardType.CREATURE),
            w("artifact").thenReturn(CardType.ARTIFACT),
            w("enchantment").thenReturn(CardType.ENCHANTMENT),
            w("land").thenReturn(CardType.LAND),
            w("planeswalker").thenReturn(CardType.PLANESWALKER),
            w("battle").thenReturn(CardType.BATTLE),
            w("instant").thenReturn(CardType.INSTANT),
            w("sorcery").thenReturn(CardType.SORCERY),
            w("kindred").thenReturn(CardType.KINDRED));

    public static final Parser<GameObjectType> GAME_OBJECT_TYPE = anyOf(
            w("permanent").thenReturn(GameObjectType.PERMANENT),
            w("spell").thenReturn(GameObjectType.SPELL),
            w("card").thenReturn(GameObjectType.CARD),
            w("token").thenReturn(GameObjectType.TOKEN),
            w("source").thenReturn(GameObjectType.SOURCE),
            w("ability").thenReturn(GameObjectType.ABILITY));

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

    // ── Counter type ───────────────────────────────────────────────────

    private static final Parser<CounterType> PT_COUNTER =
            sequence(SIGNED_INT, string("/").then(SIGNED_INT), CounterType::ptCounter);

    private static final Parser<CounterType> NAMED_COUNTER = word().suchThat(
                    w -> !w.equals("counter") && !w.equals("counters"), "counter name")
            .map(CounterType::named);

    public static final Parser<CounterType> COUNTER_TYPE = anyOf(PT_COUNTER, NAMED_COUNTER);

    // ── P/T value ──────────────────────────────────────────────────────

    public static final Parser<PtValue> PT_VALUE = sequence(INTEGER, string("/").then(INTEGER), PtValue::new);

    // ── Single type ────────────────────────────────────────────────────

    private static final Parser<Selector.SingleType> OBJECT_CARD_TYPE = sequence(
            GAME_OBJECT_TYPE.suchThat(
                    t -> t != GameObjectType.TOKEN && t != GameObjectType.SOURCE, "object type for compound"),
            CARD_TYPE,
            Selector.SingleType::objectCard);

    private static final Parser<Selector.SingleType> CARD_SINGLE = CARD_TYPE.map(Selector.SingleType::ofCard);

    private static final Parser<Selector.SingleType> OBJECT_SINGLE =
            GAME_OBJECT_TYPE.map(Selector.SingleType::ofGameObject);

    private static final Parser<String> SUBTYPE_NAME = Parser.<Enum<?>>anyOf(
                    anyOf(CreatureType.values()),
                    anyOf(LandType.values()),
                    anyOf(ArtifactType.values()),
                    anyOf(EnchantmentType.values()),
                    anyOf(SpellType.values()),
                    anyOf(PlaneswalkerType.values()),
                    anyOf(BattleType.values()))
            .map(Enum::toString);

    private static final Parser<Selector.SingleType> SUBTYPE_SINGLE = SUBTYPE_NAME.map(Selector.SingleType::ofSubtype);

    static final Parser<Selector.SingleType> SINGLE_TYPE =
            anyOf(OBJECT_CARD_TYPE, CARD_SINGLE, OBJECT_SINGLE, SUBTYPE_SINGLE);

    // ── Type expression ────────────────────────────────────────────────

    private static final Parser<Selector.TypeExpression> OR_TYPE =
            sequence(SINGLE_TYPE.followedBy(w("or")), SINGLE_TYPE, (a, b) -> Selector.TypeExpression.or(List.of(a, b)));

    private static final Parser<Selector.TypeExpression> COMPOUND_TYPE = SINGLE_TYPE
            .atLeastOnce()
            .suchThat(list -> list.size() >= 2, "compound type")
            .map(Selector.TypeExpression::compound);

    private static final Parser<Selector.TypeExpression> SINGLE_WRAP = SINGLE_TYPE.map(Selector.TypeExpression::single);

    public static final Parser<Selector.TypeExpression> TYPE_EXPRESSION = anyOf(OR_TYPE, COMPOUND_TYPE, SINGLE_WRAP);

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

    private static final Parser<Selector.Qualifier> COLOR_Q = COLOR_FILTER.map(Selector.Qualifier::color);

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
            string("non-").then(SUBTYPE_NAME).map(Selector.Qualifier::negatedSubtype);

    private static final Parser<Selector.Qualifier> STATUS_Q = anyOf(
            w("tapped").thenReturn(Selector.Qualifier.status("tapped")),
            w("untapped").thenReturn(Selector.Qualifier.status("untapped")),
            w("face-down").thenReturn(Selector.Qualifier.status("face-down")),
            w("face-up").thenReturn(Selector.Qualifier.status("face-up")));

    private static final Parser<Selector.Qualifier> COMBAT_STATUS_Q = anyOf(
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
            OTHER_Q);

    // ── With clause ────────────────────────────────────────────────────

    private static final Parser<Selector.WithClause> WITH_CLAUSE = sequence(
            anyOf(w("with").thenReturn(false), w("without").thenReturn(true)),
            word().atLeastOnce().map(words -> String.join(" ", words)),
            Selector.WithClause::new);

    // ── Controller clause ──────────────────────────────────────────────

    private static final Parser<Selector.ControllerClause> CONTROLLER_CLAUSE = anyOf(
            ciWords("you don't control").thenReturn(new Selector.ControllerClause("you don't control")),
            ciWords("you control").thenReturn(new Selector.ControllerClause("you control")),
            ciWords("an opponent controls").thenReturn(new Selector.ControllerClause("an opponent controls")),
            ciWords("each opponent controls").thenReturn(new Selector.ControllerClause("each opponent controls")));

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

    public static final Parser<Selector> SELECTOR = CORE_SELECTOR
            .optionallyFollowedBy(WITH_CLAUSE, Selector::withWithClause)
            .optionallyFollowedBy(CONTROLLER_CLAUSE, Selector::withController);
}
