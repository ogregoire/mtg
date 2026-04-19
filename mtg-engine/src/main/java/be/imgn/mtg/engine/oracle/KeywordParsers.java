package be.imgn.mtg.engine.oracle;

import static be.imgn.mtg.engine.oracle.Words.anyCiWord;
import static be.imgn.mtg.engine.oracle.Words.ciWords;
import static be.imgn.mtg.engine.oracle.Words.w;
import static com.google.common.labs.parse.Parser.anyOf;
import static com.google.common.labs.parse.Parser.sequence;
import static com.google.common.labs.parse.Parser.word;

import java.util.List;
import java.util.stream.Collectors;

import com.google.common.labs.parse.Parser;

import org.jspecify.annotations.Nullable;

/// Parsers for keyword abilities (MTG rule 702).
///
/// Each keyword produces its own {@link Ability} subtype — singleton enums
/// (e.g., {@code Ability.Flying.INSTANCE}) for parameter-less keywords, and
/// records for parameterized ones. The underlying ability type (static /
/// triggered / activated / spell) is encoded by the record's implemented
/// interface, per rule 702.Xa.
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
            kw("double strike", Ability.DoubleStrike.DOUBLE_STRIKE),
            kw("first strike", Ability.FirstStrike.FIRST_STRIKE),
            kw("split second", Ability.SplitSecond.SPLIT_SECOND),
            kw("living weapon", Ability.LivingWeapon.LIVING_WEAPON),
            kw("living metal", Ability.LivingMetal.LIVING_METAL),
            kw("battle cry", Ability.BattleCry.BATTLE_CRY),
            kw("hidden agenda", Ability.HiddenAgenda.HIDDEN_AGENDA),
            kw("umbra armor", Ability.UmbraArmor.UMBRA_ARMOR),
            kw("read ahead", Ability.ReadAhead.READ_AHEAD),
            kw("for mirrodin!", Ability.ForMirrodin.FOR_MIRRODIN));

    private static final Parser<Ability> SINGLE_WORD = anyOf(
            kw("deathtouch", Ability.Deathtouch.DEATHTOUCH),
            kw("defender", Ability.Defender.DEFENDER),
            kw("flash", Ability.Flash.FLASH),
            kw("flying", Ability.Flying.FLYING),
            kw("haste", Ability.Haste.HASTE),
            kw("hexproof", Ability.Hexproof.HEXPROOF),
            kw("indestructible", Ability.Indestructible.INDESTRUCTIBLE),
            kw("intimidate", Ability.Intimidate.INTIMIDATE),
            kw("lifelink", Ability.Lifelink.LIFELINK),
            kw("reach", Ability.Reach.REACH),
            kw("shroud", Ability.Shroud.SHROUD),
            kw("trample", Ability.Trample.TRAMPLE),
            kw("vigilance", Ability.Vigilance.VIGILANCE),
            kw("banding", Ability.Banding.BANDING),
            kw("flanking", Ability.Flanking.FLANKING),
            kw("phasing", Ability.Phasing.PHASING),
            kw("fear", Ability.Fear.FEAR),
            kw("horsemanship", Ability.Horsemanship.HORSEMANSHIP),
            kw("epic", Ability.Epic.EPIC),
            kw("convoke", Ability.Convoke.CONVOKE),
            kw("delve", Ability.Delve.DELVE),
            kw("retrace", Ability.Retrace.RETRACE),
            kw("wither", Ability.Wither.WITHER),
            kw("infect", Ability.Infect.INFECT),
            kw("menace", Ability.Menace.MENACE),
            kw("skulk", Ability.Skulk.SKULK),
            kw("devoid", Ability.Devoid.DEVOID),
            kw("fuse", Ability.Fuse.FUSE),
            kw("aftermath", Ability.Aftermath.AFTERMATH),
            kw("ascend", Ability.Ascend.ASCEND),
            kw("changeling", Ability.Changeling.CHANGELING),
            kw("decayed", Ability.Decayed.DECAYED),
            kw("compleated", Ability.Compleated.COMPLEATED),
            kw("solved", Ability.Solved.SOLVED),

            // Triggered keywords
            kw("prowess", Ability.Prowess.PROWESS),
            kw("undying", Ability.Undying.UNDYING),
            kw("persist", Ability.Persist.PERSIST),
            kw("exalted", Ability.Exalted.EXALTED),
            kw("evolve", Ability.Evolve.EVOLVE),
            kw("extort", Ability.Extort.EXTORT),
            kw("dethrone", Ability.Dethrone.DETHRONE),
            kw("soulbond", Ability.Soulbond.SOULBOND),
            kw("ingest", Ability.Ingest.INGEST),
            kw("myriad", Ability.Myriad.MYRIAD),
            kw("mentor", Ability.Mentor.MENTOR),
            kw("haunt", Ability.Haunt.HAUNT),
            kw("cascade", Ability.Cascade.CASCADE),
            kw("storm", Ability.Storm.STORM),
            kw("gravestorm", Ability.Gravestorm.GRAVESTORM),
            kw("melee", Ability.Melee.MELEE),
            kw("training", Ability.Training.TRAINING),
            kw("daybound", Ability.Daybound.DAYBOUND),
            kw("nightbound", Ability.Nightbound.NIGHTBOUND),
            kw("demonstrate", Ability.Demonstrate.DEMONSTRATE),
            kw("visit", Ability.Visit.VISIT));

    private static final Parser<Ability> SIMPLE = anyOf(MULTI_WORD, SINGLE_WORD);

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
    private static final Parser<String> QUALITY_DELIM = anyOf(
            Parser.string(",").then(ciWords("and from")),
            Parser.string(",").then(ciWords("from")),
            ciWords("and from"));

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

    /// "Equip [cost]" or "Equip—[cost]". The em-dash form carries a
    /// non-mana cost (e.g., Murderer's Axe: "Equip—Discard a card."); the
    /// plain form uses a mana cost. Both paths feed the full
    /// {@link CostParsers#COST_EXPRESSION}.
    private static final Parser<Ability> EQUIP = ciWords("equip")
            .optionallyFollowedBy("—")
            .then(CostParsers.COST_EXPRESSION)
            .map(Ability.Equip::new);

    /// "Cycling [cost]" or "Cycling—[cost]" — same shape as
    /// {@link #EQUIP}; most print as mana cost but the full cost parser
    /// handles any activation cost.
    private static final Parser<Ability> CYCLING = ciWords("cycling")
            .optionallyFollowedBy("—")
            .then(CostParsers.COST_EXPRESSION)
            .map(Ability.Cycling::new);

    /// 702.5 — "Enchant [object or player]" static ability.
    private static final Parser<Ability> ENCHANT =
            ciWords("enchant").then(word()).map(obj -> new Ability.Enchant(obj.toLowerCase()));

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
    public static final Parser<List<Ability>> KEYWORD_LIST =
            KEYWORD.atLeastOnceDelimitedBy(anyOf(Parser.string(","), w("and")), Collectors.toUnmodifiableList());
}
