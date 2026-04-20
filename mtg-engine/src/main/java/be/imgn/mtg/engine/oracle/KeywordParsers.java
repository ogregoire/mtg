package be.imgn.mtg.engine.oracle;

import static be.imgn.mtg.engine.oracle.Words.anyCiWord;
import static be.imgn.mtg.engine.oracle.Words.ciWords;
import static be.imgn.mtg.engine.oracle.Words.w;
import static be.imgn.mtg.engine.oracle.Words.words;
import static com.google.common.labs.parse.Parser.anyOf;
import static com.google.common.labs.parse.Parser.sequence;
import static com.google.common.labs.parse.Parser.word;

import java.util.List;
import java.util.stream.Collectors;

import com.google.common.labs.parse.Parser;

import org.jspecify.annotations.Nullable;

/// Parsers for keyword abilities (MTG rule 702).
///
/// Each keyword produces its own {@link Ability} subtype — consolidated into
/// {@link Ability.StaticKeyword} and {@link Ability.TriggeredKeyword} for
/// parameter-less keywords, and records for parameterized ones. The underlying
/// ability type (static / triggered / activated / spell) is encoded by the
/// implemented interface, per rule 702.Xa.
public final class KeywordParsers {

    private KeywordParsers() {}

    // ── Parameter-less keyword names ──────────────────────────────────

    /// Build a parser for a keyword name (single or multi word) that produces
    /// the singleton {@code value}.
    private static Parser<Ability> kw(String name, Ability value) {
        Parser<?> match = name.contains(" ") ? ciWords(name) : w(name);
        return match.thenReturn(value);
    }

    /// Multi-word keywords — ordered before single-word so their first word
    /// isn't matched by an unrelated single-word keyword.
    private static final Parser<Ability> MULTI_WORD = anyOf(
            kw("double strike", Ability.StaticKeyword.DOUBLE_STRIKE),
            kw("first strike", Ability.StaticKeyword.FIRST_STRIKE),
            kw("split second", Ability.StaticKeyword.SPLIT_SECOND),
            kw("living weapon", Ability.TriggeredKeyword.LIVING_WEAPON),
            kw("living metal", Ability.StaticKeyword.LIVING_METAL),
            kw("battle cry", Ability.TriggeredKeyword.BATTLE_CRY),
            kw("hidden agenda", Ability.StaticKeyword.HIDDEN_AGENDA),
            kw("umbra armor", Ability.StaticKeyword.UMBRA_ARMOR),
            kw("read ahead", Ability.StaticKeyword.READ_AHEAD),
            kw("for mirrodin!", Ability.StaticKeyword.FOR_MIRRODIN));

    private static final Parser<Ability> SINGLE_WORD = anyOf(
            kw("deathtouch", Ability.StaticKeyword.DEATHTOUCH),
            kw("defender", Ability.StaticKeyword.DEFENDER),
            kw("flash", Ability.StaticKeyword.FLASH),
            kw("flying", Ability.StaticKeyword.FLYING),
            kw("haste", Ability.StaticKeyword.HASTE),
            kw("hexproof", Ability.StaticKeyword.HEXPROOF),
            kw("indestructible", Ability.StaticKeyword.INDESTRUCTIBLE),
            kw("intimidate", Ability.StaticKeyword.INTIMIDATE),
            kw("lifelink", Ability.StaticKeyword.LIFELINK),
            kw("reach", Ability.StaticKeyword.REACH),
            kw("shroud", Ability.StaticKeyword.SHROUD),
            kw("trample", Ability.StaticKeyword.TRAMPLE),
            kw("vigilance", Ability.StaticKeyword.VIGILANCE),
            kw("banding", Ability.StaticKeyword.BANDING),
            kw("flanking", Ability.TriggeredKeyword.FLANKING),
            kw("phasing", Ability.StaticKeyword.PHASING),
            kw("fear", Ability.StaticKeyword.FEAR),
            kw("horsemanship", Ability.StaticKeyword.HORSEMANSHIP),
            kw("shadow", Ability.StaticKeyword.SHADOW),
            kw("epic", Ability.StaticKeyword.EPIC),
            kw("assist", Ability.StaticKeyword.ASSIST),
            kw("convoke", Ability.StaticKeyword.CONVOKE),
            kw("delve", Ability.StaticKeyword.DELVE),
            kw("retrace", Ability.StaticKeyword.RETRACE),
            kw("wither", Ability.StaticKeyword.WITHER),
            kw("infect", Ability.StaticKeyword.INFECT),
            kw("menace", Ability.StaticKeyword.MENACE),
            kw("skulk", Ability.StaticKeyword.SKULK),
            kw("devoid", Ability.StaticKeyword.DEVOID),
            kw("fuse", Ability.StaticKeyword.FUSE),
            kw("aftermath", Ability.StaticKeyword.AFTERMATH),
            kw("ascend", Ability.StaticKeyword.ASCEND),
            kw("changeling", Ability.StaticKeyword.CHANGELING),
            kw("decayed", Ability.StaticKeyword.DECAYED),
            kw("compleated", Ability.StaticKeyword.COMPLEATED),
            kw("solved", Ability.StaticKeyword.SOLVED),

            // Triggered keywords
            kw("prowess", Ability.TriggeredKeyword.PROWESS),
            kw("undying", Ability.TriggeredKeyword.UNDYING),
            kw("persist", Ability.TriggeredKeyword.PERSIST),
            kw("exalted", Ability.TriggeredKeyword.EXALTED),
            kw("evolve", Ability.TriggeredKeyword.EVOLVE),
            kw("extort", Ability.TriggeredKeyword.EXTORT),
            kw("dethrone", Ability.TriggeredKeyword.DETHRONE),
            kw("soulbond", Ability.TriggeredKeyword.SOULBOND),
            kw("ingest", Ability.TriggeredKeyword.INGEST),
            kw("myriad", Ability.TriggeredKeyword.MYRIAD),
            kw("mentor", Ability.TriggeredKeyword.MENTOR),
            kw("haunt", Ability.TriggeredKeyword.HAUNT),
            kw("cascade", Ability.TriggeredKeyword.CASCADE),
            kw("storm", Ability.TriggeredKeyword.STORM),
            kw("gravestorm", Ability.TriggeredKeyword.GRAVESTORM),
            kw("melee", Ability.TriggeredKeyword.MELEE),
            kw("training", Ability.TriggeredKeyword.TRAINING),
            kw("daybound", Ability.TriggeredKeyword.DAYBOUND),
            kw("nightbound", Ability.TriggeredKeyword.NIGHTBOUND),
            kw("demonstrate", Ability.TriggeredKeyword.DEMONSTRATE),
            kw("visit", Ability.TriggeredKeyword.VISIT));

    /// Parser for all parameter-less static and triggered keyword abilities.
    /// Package-visible so {@link SelectorParsers} can reuse it for the
    /// "with [keyword]" clause without duplicating the keyword list.
    static final Parser<Ability> SIMPLE = anyOf(MULTI_WORD, SINGLE_WORD);

    // ── Protection (702.16) and Hexproof from (702.11d) ───────────────

    /// A quality in a protection/hexproof clause. Returns a typed
    /// {@link ProtectionQuality}: a color, card type, subtype, variant, or a
    /// capitalized card/subtype name.
    /// "mana value [N] [or greater | or less | exactly]" — rule 702.16
    /// numeric protection quality.
    private static final Parser<ProtectionQuality> MANA_VALUE_QUALITY = sequence(
            ciWords("mana value").then(SelectorParsers.INTEGER),
            anyOf(
                    ciWords("or greater").thenReturn(ProtectionQuality.ManaValue.Comparator.OR_GREATER),
                    ciWords("or less").thenReturn(ProtectionQuality.ManaValue.Comparator.OR_LESS),
                    ciWords("exactly").thenReturn(ProtectionQuality.ManaValue.Comparator.EQUAL_TO)),
            ProtectionQuality.ManaValue::new);

    private static final Parser<ProtectionQuality> QUALITY = Parser.<ProtectionQuality>anyOf(
            MANA_VALUE_QUALITY,
            ciWords("each color").thenReturn(ProtectionQuality.Special.EACH_COLOR),
            ciWords("its colors").thenReturn(ProtectionQuality.Special.ITS_COLORS),
            // "the colors of [subject]" — dynamic quality (Empty-Shrine
            // Kannushi: "protection from the colors of permanents you
            // control.").
            ciWords("the colors of").then(SubjectParsers.SUBJECT).map(ProtectionQuality.ColorsOf::new),
            w("everything").thenReturn(ProtectionQuality.Special.EVERYTHING),
            w("monocolored").thenReturn(ProtectionQuality.Special.MONOCOLORED),
            w("multicolored").thenReturn(ProtectionQuality.Special.MULTICOLORED),
            w("colorless").thenReturn(ProtectionQuality.Special.COLORLESS),
            SelectorParsers.COLOR.map(ProtectionQuality.OfColor::new),
            SelectorParsers.CARD_TYPE.map(ProtectionQuality.OfCardType::new),
            // A capitalized single word — subtype name (e.g., Demons) or a
            // card name (e.g., Bolas). We model it as a subtype by default
            // since that's the common oracle-text usage.
            word().suchThat(s -> !s.isEmpty() && Character.isUpperCase(s.charAt(0)), "capitalized quality")
                    .map(ProtectionQuality.OfSubtype::new));

    /// Delimiter between quality items in a protection list. Accepts the
    /// simple two-item form `and from` as well as Oxford-comma three-or-more
    /// forms `, from` / `, and from` (e.g., Oversoul of Dusk: "Protection
    /// from blue, from black, and from red").
    private static final Parser<String> QUALITY_DELIM =
            anyOf(Parser.string(",").then(words("and from")), Parser.string(",").then(word("from")), words("and from"));

    private static final Parser<List<ProtectionQuality>> QUALITIES =
            QUALITY.atLeastOnceDelimitedBy(QUALITY_DELIM, Collectors.toUnmodifiableList());

    private static final Parser<Ability> PROTECTION =
            ciWords("protection from").then(QUALITIES).map(Ability.Protection::new);

    private static final Parser<Ability> HEXPROOF_FROM =
            ciWords("hexproof from").then(QUALITIES).map(Ability.HexproofFrom::new);

    // ── Parametrized keywords ─────────────────────────────────────────

    private static final Parser<List<ManaSymbol>> MANA_COST = EffectParsers.MANA_SYMBOL.atLeastOnce();

    /// 702.21 — "Ward [cost]" triggered ability.
    private static final Parser<Ability> WARD = ciWords("ward").then(MANA_COST).map(Ability.Ward::new);

    /// "Equip [subtype]? [cost]" or "Equip—[cost]". The em-dash form
    /// carries a non-mana cost (e.g., Murderer's Axe: "Equip—Discard a
    /// card."); the plain form uses a mana cost. The optional subtype
    /// restricts which creatures this Equipment can attach to (e.g.,
    /// Steelclaw Lance: "Equip Knight {1}").
    private static final Parser<Ability> EQUIP = ciWords("equip")
            .optionallyFollowedBy("—")
            .then(anyOf(
                    sequence(SelectorParsers.SUBTYPE_NAME, CostParsers.COST_EXPRESSION, Ability.Equip::new),
                    CostParsers.COST_EXPRESSION.map(Ability.Equip::new)));

    /// "Cycling [cost]" or "Cycling—[cost]" — same shape as
    /// {@link #EQUIP}; most print as mana cost but the full cost parser
    /// handles any activation cost.
    private static final Parser<Ability> CYCLING = ciWords("cycling")
            .optionallyFollowedBy("—")
            .then(CostParsers.COST_EXPRESSION)
            .map(Ability.Cycling::new);

    /// 702.5 — "Enchant [object or player]" static ability. Accepts any
    /// selector so controller clauses ("creature you control" — Emblem of
    /// the Warmind) and type restrictions ("nonland permanent") are
    /// preserved alongside the common bare-type form ("creature", "land").
    private static final Parser<Ability> ENCHANT =
            ciWords("enchant").then(SelectorParsers.SELECTOR).map(Ability.Enchant::new);

    /// 702.164 — "Toxic N" static ability.
    private static final Parser<Ability> TOXIC =
            ciWords("toxic").then(SelectorParsers.INTEGER).map(Ability.Toxic::new);

    // ── Landwalk (702.14) — "[type]walk" static evasion ───────────────

    /// The basic-land-type `[subtype]walk` form or the generic `landwalk`.
    private static final Parser<String> WALK_WORD =
            anyCiWord("plainswalk", "islandwalk", "swampwalk", "mountainwalk", "forestwalk", "landwalk");

    /// Qualifier that can precede `landwalk` or a basic walk (rule 702.14a).
    private static final Parser<String> WALK_QUALIFIER =
            anyCiWord("legendary", "snow", "basic", "nonbasic", "artifact");

    private static @Nullable LandType basicFromWalk(String walkWord) {
        return switch (walkWord.toLowerCase()) {
            case "plainswalk" -> LandType.PLAINS;
            case "islandwalk" -> LandType.ISLAND;
            case "swampwalk" -> LandType.SWAMP;
            case "mountainwalk" -> LandType.MOUNTAIN;
            case "forestwalk" -> LandType.FOREST;
            case "landwalk" -> null;
            default -> throw new IllegalStateException("unexpected walk: " + walkWord);
        };
    }

    private static LandSelector toSelector(@Nullable String qualifier, String walkWord) {
        var subtype = basicFromWalk(walkWord);
        Supertype supertype = null;
        boolean nonbasic = false;
        CardType cardType = null;
        if (qualifier != null) {
            switch (qualifier.toLowerCase()) {
                case "legendary" -> supertype = Supertype.LEGENDARY;
                case "snow" -> supertype = Supertype.SNOW;
                case "basic" -> supertype = Supertype.BASIC;
                case "nonbasic" -> nonbasic = true;
                case "artifact" -> cardType = CardType.ARTIFACT;
                default -> {
                    /* unreachable given WALK_QUALIFIER */
                }
            }
        }
        return new LandSelector(supertype, nonbasic, cardType, subtype);
    }

    private static final Parser<Ability> LANDWALK = anyOf(
            sequence(WALK_QUALIFIER, WALK_WORD, (q, w) -> new Ability.Landwalk(toSelector(q, w))),
            WALK_WORD.map(w -> new Ability.Landwalk(toSelector(null, w))));

    // ── Assembled keyword parser ──────────────────────────────────────

    public static final Parser<Ability> KEYWORD = Parser.<Ability>anyOf(
                    PROTECTION, HEXPROOF_FROM, WARD, EQUIP, CYCLING, ENCHANT, TOXIC, LANDWALK, SIMPLE)
            .optionallyFollowedBy(OracleParser.REMINDER, (k, r) -> k);

    /// List of one or more keyword abilities on a single line (rule 702.1:
    /// each keyword on a line is a separate ability). Keywords are joined by
    /// "," or "and" — either "Flying, trample, haste" or "flying and haste".
    /// Each keyword may carry its own trailing `(reminder text)`.
    public static final Parser<List<Ability>> KEYWORD_LIST = KEYWORD.atLeastOnceDelimitedBy(
            anyOf(
                    // Oxford-comma tail ", and" first so the "," and "and"
                    // aren't split into two delimiters with no keyword
                    // between them (Chariot of Victory: "has first strike,
                    // trample, and haste.").
                    sequence(Parser.string(","), w("and"), (_, _) -> ", and"),
                    Parser.string(","),
                    // Some cards (Ancient Spider: "First strike; reach")
                    // use a semicolon between keywords.
                    Parser.string(";"),
                    w("and")),
            Collectors.toUnmodifiableList());
}
