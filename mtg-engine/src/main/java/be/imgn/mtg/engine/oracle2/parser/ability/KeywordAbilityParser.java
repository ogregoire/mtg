package be.imgn.mtg.engine.oracle2.parser.ability;

import static be.imgn.mtg.engine.oracle2.parser.NumberParser.INTEGER;
import static be.imgn.mtg.engine.oracle2.parser.Parsers.phrase;
import static com.google.common.labs.parse.Parser.anyOf;
import static com.google.common.labs.parse.Parser.or;
import static com.google.common.labs.parse.Parser.sequence;
import static com.google.common.labs.parse.Parser.string;

import java.util.List;
import java.util.stream.Stream;

import com.google.common.labs.parse.Parser;

import be.imgn.mtg.engine.oracle2.domain.Amount;
import be.imgn.mtg.engine.oracle2.domain.CardType;
import be.imgn.mtg.engine.oracle2.domain.ability.Ability;
import be.imgn.mtg.engine.oracle2.parser.selector.AbilitySelectorParser;
import be.imgn.mtg.engine.oracle2.parser.selector.ColorSelectorParser;
import be.imgn.mtg.engine.oracle2.parser.selector.SelectorParser;
import be.imgn.mtg.engine.oracle2.parser.selector.TypeSelectorParser;

/// Parser for keyword abilities ({@mtg.rule 702}). Each keyword is a
/// shortcut for an ability of one of the four types ({@mtg.rule 113.3}),
/// so the parser produces a [Ability] directly — the resulting
/// record's parent interface ([Ability.Static], [Ability.Triggered],
/// [Ability.Activated]) carries the typing.
///
/// Two grammar shapes:
/// 1. **Parameter-less** — a single bare-word keyword
///    ("Flying", "Trample", "Prowess"). The phrase→enum table lives
///    in [AbilitySelectorParser#ABILITY_KEYWORD] and is reused here so
///    the canonical printed form is one source of truth.
/// 2. **Parameterised** — keyword + argument(s) ("Equip {2}", "Ward
///    {1}{B}", "Toxic 1", "Reinforce 2—{2}{W}"). One per-record
///    parser per parametrized keyword.
///
/// Deferred (require selector / type-name parsing not yet wired
/// here): [Ability.Landwalk], [Ability.Enchant], [Ability.Affinity].
/// They land when the supporting parsers do.
public final class KeywordAbilityParser {
    private KeywordAbilityParser() {}

    // ── Parameterised keywords (rule 702) ──────────────────────────

    /// `Equip [cost]` ({@mtg.rule 702.6}). Restriction-narrowed forms
    /// ("Equip Knight {1}", "Equip legendary creature {3}") are
    /// deferred — Restriction subtype/supertype parsing lands later.
    public static final Parser<Ability.Equip> EQUIP =
            phrase("Equip").then(CostParser.MANA_COST).map(Ability.Equip::new);

    /// `Enchant [selector]` ({@mtg.rule 702.5}). Aura attachment
    /// restriction — Evil Presence: "Enchant land". The selector is
    /// the full [SelectorParser#SELECTOR] so "Enchant creature you
    /// control" / "Enchant player" forms fit when they appear.
    public static final Parser<Ability.Enchant> ENCHANT =
            phrase("Enchant").then(SelectorParser.SELECTOR).map(Ability.Enchant::new);

    /// `Cycling [cost]` ({@mtg.rule 702.29}).
    public static final Parser<Ability.Cycling> CYCLING =
            phrase("Cycling").then(CostParser.MANA_COST).map(Ability.Cycling::new);

    /// `Outlast [cost]` ({@mtg.rule 702.107}).
    public static final Parser<Ability.Outlast> OUTLAST =
            phrase("Outlast").then(CostParser.MANA_COST).map(Ability.Outlast::new);

    /// `Encore [cost]` ({@mtg.rule 702.141}).
    public static final Parser<Ability.Encore> ENCORE =
            phrase("Encore").then(CostParser.MANA_COST).map(Ability.Encore::new);

    /// `Ward [mana cost]` ({@mtg.rule 702.21}) — the common form.
    /// Em-dash form ("Ward—Pay 7 life.", "Ward—Discard a card.")
    /// carries an arbitrary non-mana [CostParser#COST] body
    /// terminated by a period; that arm is tried first because both
    /// share the `Ward` prefix and the em-dash is the disambiguator.
    public static final Parser<Ability.Ward> WARD = anyOf(
            phrase("Ward")
                    .then(string("—"))
                    .then(CostParser.COST)
                    .followedBy(phrase("."))
                    .map(Ability.Ward::new),
            phrase("Ward").then(CostParser.MANA_COST).map(Ability.Ward::new));

    /// "Crew N" ({@mtg.rule 702.122}). The aggregate-power threshold
    /// is always an integer literal in oracle text; wrapped in
    /// [Amount.Exact] to match the [Ability.Crew] field type.
    public static final Parser<Ability.Crew> CREW =
            phrase("Crew").then(INTEGER).map(n -> new Ability.Crew(new Amount.Exact(n)));

    /// `Reinforce N—[cost]` ({@mtg.rule 702.77}). The em-dash is
    /// mandatory in oracle text — Reinforce never appears with a
    /// plain mana cost suffix.
    public static final Parser<Ability.Reinforce> REINFORCE =
            sequence(phrase("Reinforce").then(INTEGER), string("—").then(CostParser.MANA_COST), Ability.Reinforce::new);

    /// "Toxic N" ({@mtg.rule 702.164}). Always carries a level in
    /// oracle text — null `n` is reserved for the
    /// "with toxic" condition-check shape (parsed elsewhere).
    public static final Parser<Ability.Toxic> TOXIC =
            phrase("Toxic").then(INTEGER).map(Ability.Toxic::new);

    /// "Support N" ({@mtg.rule 701.41}).
    public static final Parser<Ability.Support> SUPPORT =
            phrase("Support").then(INTEGER).map(Ability.Support::new);

    /// "Afflict N" ({@mtg.rule 702.130}).
    public static final Parser<Ability.Afflict> AFFLICT =
            phrase("Afflict").then(INTEGER).map(Ability.Afflict::new);

    /// "Bushido N" ({@mtg.rule 702.45}).
    public static final Parser<Ability.Bushido> BUSHIDO =
            phrase("Bushido").then(INTEGER).map(Ability.Bushido::new);

    /// "Afterlife N" ({@mtg.rule 702.135}).
    public static final Parser<Ability.Afterlife> AFTERLIFE =
            phrase("Afterlife").then(INTEGER).map(Ability.Afterlife::new);

    /// "Firebending N" — Avatar universes-beyond keyword.
    public static final Parser<Ability.Firebending> FIREBENDING =
            phrase("Firebending").then(INTEGER).map(Ability.Firebending::new);

    /// `Protection from [quality]` ({@mtg.rule 702.16}). Quality is a
    /// color ("red", "blue", …) or a subtype ("Goblins", "Elves",
    /// "Kavu"). Color tried first so subtype-named colors don't
    /// shadow them (no current collisions, but explicit order
    /// documents the priority).
    private static final Parser<Ability.Protection.Quality.OfColor> PROTECTION_COLOR =
            ColorSelectorParser.COLOR_NAME.map(Ability.Protection.Quality.OfColor::new);

    private static final Parser<Ability.Protection.Quality.OfSubtype> PROTECTION_SUBTYPE =
            TypeSelectorParser.SUBTYPE_IS.map(is -> new Ability.Protection.Quality.OfSubtype(is.subtype()));

    /// "protection from creatures", "protection from artifacts" — the
    /// plural form is handled by each [CardType]'s `text()` template
    /// (`"Creature(s)"`, `"Artifact(s)"`).
    private static final Parser<Ability.Protection.Quality.OfCardType> PROTECTION_CARD_TYPE = Stream.of(
                    CardType.values())
            .map(t -> phrase(t.text()).thenReturn(new Ability.Protection.Quality.OfCardType(t)))
            .collect(or());

    /// "protection from monocolored" / "protection from multicolored"
    /// / "protection from each color" ({@mtg.rule 702.16h-i}). Bare-
    /// word qualifier with no payload.
    private static final Parser<Ability.Protection.Quality.Standard> PROTECTION_STANDARD = anyOf(
            phrase("monocolored").thenReturn(Ability.Protection.Quality.Standard.MONOCOLORED),
            phrase("multicolored").thenReturn(Ability.Protection.Quality.Standard.MULTICOLORED),
            phrase("each color").thenReturn(Ability.Protection.Quality.Standard.EACH_COLOR));

    private static final Parser<Ability.Protection.Quality> PROTECTION_QUALITY =
            anyOf(PROTECTION_COLOR, PROTECTION_CARD_TYPE, PROTECTION_STANDARD, PROTECTION_SUBTYPE);

    /// "Protection from X (and from Y)*" — a single Quality when only
    /// one is listed, [Ability.Protection.Quality.AllOf] when two or
    /// more (Kitsune Riftwalker: "Protection from Spirits and from
    /// Arcane"). The "and from" delimiter is consumed atomically so
    /// the prefix-share is explicit.
    private static final Parser<Ability.Protection.Quality> PROTECTION_QUALITIES = PROTECTION_QUALITY
            .atLeastOnceDelimitedBy("and from")
            .map(qs -> qs.size() == 1 ? qs.getFirst() : new Ability.Protection.Quality.AllOf(qs));

    public static final Parser<Ability.Protection> PROTECTION =
            phrase("Protection from").then(PROTECTION_QUALITIES).map(Ability.Protection::new);

    // ── Top-level dispatch ─────────────────────────────────────────

    /// One keyword ability. Parameterised arms come first (their
    /// keyword names — Toxic, Ward, Equip, … — never overlap with
    /// no-param keywords, but explicit ordering documents the
    /// priority and leaves the no-param table as the catch-all).
    public static final Parser<Ability> KEYWORD = anyOf(
            EQUIP,
            ENCHANT,
            CYCLING,
            OUTLAST,
            ENCORE,
            WARD,
            CREW,
            REINFORCE,
            TOXIC,
            SUPPORT,
            AFFLICT,
            BUSHIDO,
            AFTERLIFE,
            FIREBENDING,
            PROTECTION,
            AbilitySelectorParser.ABILITY_KEYWORD);

    /// Comma-separated keyword list — "Flying, vigilance",
    /// "Flying, trample, haste". Single keyword paragraphs collapse
    /// to a singleton list.
    public static final Parser<List<Ability>> KEYWORD_LIST = KEYWORD.atLeastOnceDelimitedBy(",");
}
