package be.imgn.mtg.engine.oracle.parser;

import static be.imgn.mtg.engine.oracle.parser.SelectorParsers.CARD_TYPE;
import static be.imgn.mtg.engine.oracle.parser.SelectorParsers.COLOR;
import static be.imgn.mtg.engine.oracle.parser.SelectorParsers.INTEGER;
import static be.imgn.mtg.engine.oracle.parser.SelectorParsers.SELECTOR;
import static be.imgn.mtg.engine.oracle.parser.SelectorParsers.SUBTYPE;
import static be.imgn.mtg.engine.oracle.parser.Words.phrase;
import static com.google.common.labs.parse.Parser.anyOf;
import static com.google.common.labs.parse.Parser.sequence;
import static com.google.common.labs.parse.Parser.string;
import static com.google.common.labs.parse.Parser.word;

import java.util.List;
import java.util.stream.Collectors;

import com.google.common.labs.parse.Parser;

import org.jspecify.annotations.Nullable;

import be.imgn.mtg.engine.oracle.domain.*;

/// Parsers for keyword abilities (MTG rule 702).
///
/// Each keyword produces its own [Ability] subtype — consolidated into
/// [Ability.StaticKeyword] and [Ability.TriggeredKeyword] for
/// parameter-less keywords, and records for parameterized ones. The underlying
/// ability type (static / triggered / activated / spell) is encoded by the
/// implemented interface, per rule 702.Xa.
public final class KeywordParsers {

    private KeywordParsers() {}

    // ── Parameter-less keyword names ──────────────────────────────────

    /// Parser for all parameter-less static and triggered keyword abilities.
    /// Multi-word keywords are listed first so their leading word isn't
    /// consumed by a single-word entry. Each keyword is written with a
    /// capitalized first letter so [Words#phrase] matches both
    /// sentence-start ("Flying") and mid-sentence ("flying") forms.
    /// Package-visible so [SelectorParsers] can reuse it.
    static final Parser<Ability> SIMPLE = anyOf(
            phrase("Double strike").thenReturn(Ability.StaticKeyword.DOUBLE_STRIKE),
            phrase("First strike").thenReturn(Ability.StaticKeyword.FIRST_STRIKE),
            phrase("Split second").thenReturn(Ability.StaticKeyword.SPLIT_SECOND),
            phrase("Living weapon").thenReturn(Ability.TriggeredKeyword.LIVING_WEAPON),
            phrase("Living metal").thenReturn(Ability.StaticKeyword.LIVING_METAL),
            phrase("Battle cry").thenReturn(Ability.TriggeredKeyword.BATTLE_CRY),
            phrase("Hidden agenda").thenReturn(Ability.StaticKeyword.HIDDEN_AGENDA),
            phrase("Umbra armor").thenReturn(Ability.StaticKeyword.UMBRA_ARMOR),
            phrase("Read ahead").thenReturn(Ability.StaticKeyword.READ_AHEAD),
            phrase("For mirrodin").followedBy(Parser.one('!')).thenReturn(Ability.StaticKeyword.FOR_MIRRODIN),
            phrase("Deathtouch").thenReturn(Ability.StaticKeyword.DEATHTOUCH),
            phrase("Defender").thenReturn(Ability.StaticKeyword.DEFENDER),
            phrase("Flash").thenReturn(Ability.StaticKeyword.FLASH),
            phrase("Flying").thenReturn(Ability.StaticKeyword.FLYING),
            phrase("Haste").thenReturn(Ability.StaticKeyword.HASTE),
            phrase("Hexproof").thenReturn(Ability.StaticKeyword.HEXPROOF),
            phrase("Indestructible").thenReturn(Ability.StaticKeyword.INDESTRUCTIBLE),
            phrase("Intimidate").thenReturn(Ability.StaticKeyword.INTIMIDATE),
            phrase("Lifelink").thenReturn(Ability.StaticKeyword.LIFELINK),
            phrase("Reach").thenReturn(Ability.StaticKeyword.REACH),
            phrase("Shroud").thenReturn(Ability.StaticKeyword.SHROUD),
            phrase("Trample").thenReturn(Ability.StaticKeyword.TRAMPLE),
            phrase("Vigilance").thenReturn(Ability.StaticKeyword.VIGILANCE),
            phrase("Banding").thenReturn(Ability.StaticKeyword.BANDING),
            phrase("Flanking").thenReturn(Ability.TriggeredKeyword.FLANKING),
            phrase("Phasing").thenReturn(Ability.StaticKeyword.PHASING),
            phrase("Fear").thenReturn(Ability.StaticKeyword.FEAR),
            phrase("Horsemanship").thenReturn(Ability.StaticKeyword.HORSEMANSHIP),
            phrase("Shadow").thenReturn(Ability.StaticKeyword.SHADOW),
            phrase("Epic").thenReturn(Ability.StaticKeyword.EPIC),
            phrase("Assist").thenReturn(Ability.StaticKeyword.ASSIST),
            phrase("Convoke").thenReturn(Ability.StaticKeyword.CONVOKE),
            phrase("Delve").thenReturn(Ability.StaticKeyword.DELVE),
            phrase("Retrace").thenReturn(Ability.StaticKeyword.RETRACE),
            phrase("Wither").thenReturn(Ability.StaticKeyword.WITHER),
            phrase("Infect").thenReturn(Ability.StaticKeyword.INFECT),
            phrase("Menace").thenReturn(Ability.StaticKeyword.MENACE),
            phrase("Skulk").thenReturn(Ability.StaticKeyword.SKULK),
            phrase("Devoid").thenReturn(Ability.StaticKeyword.DEVOID),
            phrase("Fuse").thenReturn(Ability.StaticKeyword.FUSE),
            phrase("Aftermath").thenReturn(Ability.StaticKeyword.AFTERMATH),
            phrase("Ascend").thenReturn(Ability.StaticKeyword.ASCEND),
            phrase("Riot").thenReturn(Ability.StaticKeyword.RIOT),
            phrase("Changeling").thenReturn(Ability.StaticKeyword.CHANGELING),
            phrase("Decayed").thenReturn(Ability.StaticKeyword.DECAYED),
            phrase("Compleated").thenReturn(Ability.StaticKeyword.COMPLEATED),
            phrase("Solved").thenReturn(Ability.StaticKeyword.SOLVED),
            phrase("Prowess").thenReturn(Ability.TriggeredKeyword.PROWESS),
            phrase("Undying").thenReturn(Ability.TriggeredKeyword.UNDYING),
            phrase("Persist").thenReturn(Ability.TriggeredKeyword.PERSIST),
            phrase("Exalted").thenReturn(Ability.TriggeredKeyword.EXALTED),
            phrase("Evolve").thenReturn(Ability.TriggeredKeyword.EVOLVE),
            phrase("Extort").thenReturn(Ability.TriggeredKeyword.EXTORT),
            phrase("Dethrone").thenReturn(Ability.TriggeredKeyword.DETHRONE),
            phrase("Soulbond").thenReturn(Ability.TriggeredKeyword.SOULBOND),
            phrase("Ingest").thenReturn(Ability.TriggeredKeyword.INGEST),
            phrase("Myriad").thenReturn(Ability.TriggeredKeyword.MYRIAD),
            phrase("Mentor").thenReturn(Ability.TriggeredKeyword.MENTOR),
            phrase("Haunt").thenReturn(Ability.TriggeredKeyword.HAUNT),
            phrase("Cascade").thenReturn(Ability.TriggeredKeyword.CASCADE),
            phrase("Storm").thenReturn(Ability.TriggeredKeyword.STORM),
            phrase("Gravestorm").thenReturn(Ability.TriggeredKeyword.GRAVESTORM),
            phrase("Melee").thenReturn(Ability.TriggeredKeyword.MELEE),
            phrase("Training").thenReturn(Ability.TriggeredKeyword.TRAINING),
            phrase("Daybound").thenReturn(Ability.TriggeredKeyword.DAYBOUND),
            phrase("Nightbound").thenReturn(Ability.TriggeredKeyword.NIGHTBOUND),
            phrase("Demonstrate").thenReturn(Ability.TriggeredKeyword.DEMONSTRATE),
            phrase("Visit").thenReturn(Ability.TriggeredKeyword.VISIT));

    // ── Protection (702.16) and Hexproof from (702.11d) ───────────────

    /// A quality in a protection/hexproof clause. Returns a typed
    /// [ProtectionQuality]: a color, card type, subtype, variant, or a
    /// capitalized card/subtype name.
    /// "mana value [N] [or greater | or less]?" — rule 702.16 numeric
    /// protection quality. A bare integer with no trailing comparator
    /// is the exact-equals form (e.g., "with mana value 3").
    private static final Parser<ProtectionQuality> MANA_VALUE_QUALITY = phrase("mana value")
            .then(INTEGER)
            .map(ProtectionQuality.ManaValue::new)
            .optionallyFollowedBy(
                    anyOf(
                            phrase("or greater").thenReturn(ProtectionQuality.ManaValue.Comparator.OR_GREATER),
                            phrase("or less").thenReturn(ProtectionQuality.ManaValue.Comparator.OR_LESS)),
                    ProtectionQuality.ManaValue::withComparator)
            .map(x -> x); // widen for typing

    private static final Parser<ProtectionQuality> QUALITY = Parser.anyOf(
            MANA_VALUE_QUALITY,
            phrase("each color").thenReturn(ProtectionQuality.Special.EACH_COLOR),
            phrase("its colors").thenReturn(ProtectionQuality.Special.ITS_COLORS),
            // "the colors of [subject]" — dynamic quality (Empty-Shrine
            // Kannushi: "protection from the colors of permanents you
            // control.").
            phrase("the colors of").then(SubjectParsers.SUBJECT).map(ProtectionQuality.ColorsOf::new),
            // "the color of [chooser]'s choice" — Stave Off. Singular
            // "color" keeps this distinct from the plural ColorsOf
            // arm above, so arm order between the two is irrelevant.
            phrase("the color of")
                    .then(anyOf(
                            word("your").thenReturn(Subject.PlayerRef.YOU),
                            word("their").thenReturn(Subject.PlayerRef.THEY),
                            phrase("an opponent's").thenReturn(Subject.PlayerRef.AN_OPPONENT)))
                    .followedBy(word("choice"))
                    .map(ProtectionQuality.ChosenColor::new),
            word("everything").thenReturn(ProtectionQuality.Special.EVERYTHING),
            word("monocolored").thenReturn(ProtectionQuality.Special.MONOCOLORED),
            word("multicolored").thenReturn(ProtectionQuality.Special.MULTICOLORED),
            word("colorless").thenReturn(ProtectionQuality.Special.COLORLESS),
            COLOR.map(ProtectionQuality.OfColor::new),
            // "non-<subtype> <cardtype>" — negated-subtype within a
            // card type (Spare from Evil: "protection from non-Human
            // creatures"). Must precede bare CARD_TYPE / SUBTYPE so
            // the "non-" prefix is captured here, not fed into the
            // selector qualifier path.
            sequence(
                    anyOf(string("non-"), string("Non-")).then(SUBTYPE),
                    CARD_TYPE,
                    ProtectionQuality.OfNonSubtypeOfCardType::new),
            CARD_TYPE.map(ProtectionQuality.OfCardType::new),
            // Known subtype (e.g., DEMON, GOBLIN) via the SUBTYPE table —
            // typed Subtype constant instead of a free-text capitalized
            // word, so downstream code can pattern-match.
            SUBTYPE.map(ProtectionQuality.OfSubtype::new));

    /// Delimiter between quality items in a protection list. Accepts the
    /// simple two-item form `and from` as well as Oxford-comma three-or-more
    /// forms `, from` / `, and from` (e.g., Oversoul of Dusk: "Protection
    /// from blue, from black, and from red").
    private static final Parser<String> QUALITY_DELIM = anyOf(
            Parser.string(",").then(phrase("and from")), Parser.string(",").then(word("from")), phrase("and from"));

    private static final Parser<List<ProtectionQuality>> QUALITIES =
            QUALITY.atLeastOnceDelimitedBy(QUALITY_DELIM, Collectors.toUnmodifiableList());

    private static final Parser<Ability> PROTECTION =
            phrase("Protection from").then(QUALITIES).map(Ability.Protection::new);

    private static final Parser<Ability> HEXPROOF_FROM =
            phrase("Hexproof from").then(QUALITIES).map(Ability.HexproofFrom::new);

    // ── Parametrized keywords ─────────────────────────────────────────

    private static final Parser<List<ManaSymbol>> MANA_COST = EffectParsers.MANA_SYMBOL.atLeastOnce();

    /// 702.21 — "Ward [cost]" triggered ability. Most print as a mana cost
    /// ("Ward {2}") but the em-dash form carries a non-mana cost
    /// (Sire of Seven Deaths: "Ward—Pay 7 life.").
    private static final Parser<Ability> WARD = phrase("Ward")
            .optionallyFollowedBy("—")
            .then(CostParsers.COST_EXPRESSION)
            .map(Ability.Ward::new);

    /// 702.115 — "Support N" triggered ability (Lead by Example:
    /// "Support 2."). The count is the upper bound on +1/+1-counter
    /// targets on ETB.
    private static final Parser<Ability> SUPPORT =
            phrase("Support").then(INTEGER).map(Ability.Support::new);

    /// 702.130 — "Afflict N" triggered ability (Khenra Eternal:
    /// "Afflict 1."). Defending player loses N life when this creature
    /// becomes blocked.
    private static final Parser<Ability> AFFLICT =
            phrase("Afflict").then(INTEGER).map(Ability.Afflict::new);

    /// "Firebending N" triggered ability (Mai and Zuko).
    private static final Parser<Ability> FIREBENDING =
            phrase("Firebending").then(INTEGER).map(Ability.Firebending::new);

    /// "Equip [subtype]? [cost]" or "Equip—[cost]". The em-dash form
    /// carries a non-mana cost (e.g., Murderer's Axe: "Equip—Discard a
    /// card."); the plain form uses a mana cost. The optional subtype
    /// restricts which creatures this Equipment can attach to (e.g.,
    /// Steelclaw Lance: "Equip Knight {1}").
    private static final Parser<Ability> EQUIP = phrase("Equip")
            .optionallyFollowedBy("—")
            .then(anyOf(
                    sequence(SUBTYPE, CostParsers.COST_EXPRESSION, Ability.Equip::new),
                    CostParsers.COST_EXPRESSION.map(Ability.Equip::new)));

    /// "Cycling [cost]" or "Cycling—[cost]" — same shape as
    /// [#EQUIP]; most print as mana cost but the full cost parser
    /// handles any activation cost.
    private static final Parser<Ability> CYCLING = phrase("Cycling")
            .optionallyFollowedBy("—")
            .then(CostParsers.COST_EXPRESSION)
            .map(Ability.Cycling::new);

    /// 702.5 — "Enchant [object or player]" static ability. Accepts any
    /// selector so controller clauses ("creature you control" — Emblem of
    /// the Warmind) and type restrictions ("nonland permanent") are
    /// preserved alongside the common bare-type form ("creature", "land").
    private static final Parser<Ability> ENCHANT =
            phrase("Enchant").then(SELECTOR).map(Ability.Enchant::new);

    /// 702.164 — "Toxic N" static ability.
    private static final Parser<Ability> TOXIC = phrase("Toxic").then(INTEGER).map(Ability.Toxic::new);

    // ── Landwalk (702.14) — "[type]walk" static evasion ───────────────

    /// The basic-land-type `[subtype]walk` form or the generic `landwalk`.
    private static final Parser<String> WALK_WORD = anyOf(
            phrase("Plainswalk").thenReturn("plainswalk"),
            phrase("Islandwalk").thenReturn("islandwalk"),
            phrase("Swampwalk").thenReturn("swampwalk"),
            phrase("Mountainwalk").thenReturn("mountainwalk"),
            phrase("Forestwalk").thenReturn("forestwalk"),
            // Non-basic land-type walks (Desert Nomads: "Desertwalk").
            phrase("Desertwalk").thenReturn("desertwalk"),
            phrase("Landwalk").thenReturn("landwalk"));

    /// Qualifier that can precede `landwalk` or a basic walk (rule 702.14a).
    private static final Parser<String> WALK_QUALIFIER = anyOf(
            phrase("Legendary").thenReturn("legendary"),
            phrase("Snow").thenReturn("snow"),
            phrase("Basic").thenReturn("basic"),
            phrase("Nonbasic").thenReturn("nonbasic"),
            phrase("Artifact").thenReturn("artifact"));

    private static @Nullable LandType basicFromWalk(String walkWord) {
        return switch (walkWord.toLowerCase()) {
            case "plainswalk" -> LandType.PLAINS;
            case "islandwalk" -> LandType.ISLAND;
            case "swampwalk" -> LandType.SWAMP;
            case "mountainwalk" -> LandType.MOUNTAIN;
            case "forestwalk" -> LandType.FOREST;
            case "desertwalk" -> LandType.DESERT;
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
                    PROTECTION,
                    HEXPROOF_FROM,
                    WARD,
                    SUPPORT,
                    AFFLICT,
                    FIREBENDING,
                    EQUIP,
                    CYCLING,
                    ENCHANT,
                    TOXIC,
                    LANDWALK,
                    SIMPLE)
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
                    sequence(Parser.string(","), phrase("and"), (_, _) -> ", and"),
                    Parser.string(","),
                    // Some cards (Ancient Spider: "First strike; reach")
                    // use a semicolon between keywords.
                    Parser.string(";"),
                    phrase("and")),
            Collectors.toUnmodifiableList());
}
