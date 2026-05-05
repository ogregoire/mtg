package be.imgn.mtg.engine.oracle2.parser.selector;

import static be.imgn.mtg.engine.oracle2.parser.Parsers.andList;
import static be.imgn.mtg.engine.oracle2.parser.Parsers.orList;
import static be.imgn.mtg.engine.oracle2.parser.Parsers.phrase;
import static com.google.common.labs.parse.Parser.anyOf;

import com.google.common.labs.parse.Parser;

import be.imgn.mtg.engine.oracle2.domain.Ability;
import be.imgn.mtg.engine.oracle2.domain.selector.AbilitySelector;
import be.imgn.mtg.engine.oracle2.domain.selector.ObjectPropertySelector;

/// Parser for [AbilitySelector]. Produces an
/// [ObjectPropertySelector] (not a bare [AbilitySelector]) because
/// compound `with X or Y` / `with X and Y` phrases distribute the
/// `with` and yield [ObjectPropertySelector.AnyOf] /
/// [ObjectPropertySelector.AllOf].
///
/// **The phrase→enum mapping for keyword names lives entirely in
/// this file.** [Ability]'s domain enums hold no parsing metadata;
/// the [#ABILITY_KEYWORD] table below is the single source of truth
/// for the canonical printed form of each parameter-less keyword,
/// shared with
/// [be.imgn.mtg.engine.oracle2.parser.KeywordAbilityParser] for the
/// keyword-ability paragraph dispatch.
///
/// Top-level shapes:
/// 1. `with no abilities` ({@mtg.rule 113.6}) — produces
///    [AbilitySelector.HasNoAbilities].
/// 2. `with X and Y[, and Z]` — Oxford-comma `and` list, ≥2
///    keywords. Distributes `with` across each keyword and
///    combines via [ObjectPropertySelector.AllOf] of
///    [AbilitySelector.Has].
/// 3. `with X or Y[, or Z]` — same shape, [ObjectPropertySelector.AnyOf].
/// 4. `with X` — single keyword → [AbilitySelector.Has].
/// 5. `without X or Y[, or Z]` — negation list. Combines via
///    [AllOf] of [HasNot] per De Morgan: `NOT(K1 OR K2) = NOT K1
///    AND NOT K2` (Stormtide Leviathan: "creatures without flying
///    or islandwalk"). Note: oracle never uses `and` after
///    `without` — only `or` — so there is no `WITHOUT_AND_COMPOUND`
///    arm.
/// 6. `without X` — single keyword → [AbilitySelector.HasNot].
public final class AbilitySelectorParser {
    private AbilitySelectorParser() {}

    /// Phrase → keyword constant table. Exhaustive over
    /// [Ability.StaticKeyword] and [Ability.TriggeredKeyword]. Order
    /// is alphabetical; first letter of each phrase is capitalised so
    /// every entry is `phrase()`-title-or-lower (matches both
    /// sentence-start "Flying" and mid-sentence "flying"). The same
    /// table is consumed both by [AbilitySelectorParser] (always
    /// mid-sentence: "with flying") and by
    /// [be.imgn.mtg.engine.oracle2.parser.KeywordAbilityParser]
    /// (frequently sentence-start on bare-keyword paragraphs:
    /// "Flying"), hence the broadened casing.
    public static final Parser<Ability> ABILITY_KEYWORD = anyOf(
            phrase("Aftermath").thenReturn(Ability.StaticKeyword.AFTERMATH),
            phrase("Ascend").thenReturn(Ability.StaticKeyword.ASCEND),
            phrase("Assist").thenReturn(Ability.StaticKeyword.ASSIST),
            phrase("Banding").thenReturn(Ability.StaticKeyword.BANDING),
            phrase("Battle cry").thenReturn(Ability.TriggeredKeyword.BATTLE_CRY),
            phrase("Cascade").thenReturn(Ability.TriggeredKeyword.CASCADE),
            phrase("Changeling").thenReturn(Ability.StaticKeyword.CHANGELING),
            phrase("Compleated").thenReturn(Ability.StaticKeyword.COMPLEATED),
            phrase("Convoke").thenReturn(Ability.StaticKeyword.CONVOKE),
            phrase("Daybound").thenReturn(Ability.TriggeredKeyword.DAYBOUND),
            phrase("Deathtouch").thenReturn(Ability.StaticKeyword.DEATHTOUCH),
            phrase("Decayed").thenReturn(Ability.StaticKeyword.DECAYED),
            phrase("Defender").thenReturn(Ability.StaticKeyword.DEFENDER),
            phrase("Delve").thenReturn(Ability.StaticKeyword.DELVE),
            phrase("Demonstrate").thenReturn(Ability.TriggeredKeyword.DEMONSTRATE),
            phrase("Dethrone").thenReturn(Ability.TriggeredKeyword.DETHRONE),
            phrase("Devoid").thenReturn(Ability.StaticKeyword.DEVOID),
            phrase("Double strike").thenReturn(Ability.StaticKeyword.DOUBLE_STRIKE),
            phrase("Epic").thenReturn(Ability.StaticKeyword.EPIC),
            phrase("Evolve").thenReturn(Ability.TriggeredKeyword.EVOLVE),
            phrase("Exalted").thenReturn(Ability.TriggeredKeyword.EXALTED),
            phrase("Extort").thenReturn(Ability.TriggeredKeyword.EXTORT),
            phrase("Fear").thenReturn(Ability.StaticKeyword.FEAR),
            phrase("First strike").thenReturn(Ability.StaticKeyword.FIRST_STRIKE),
            phrase("Flanking").thenReturn(Ability.TriggeredKeyword.FLANKING),
            phrase("Flash").thenReturn(Ability.StaticKeyword.FLASH),
            phrase("Flying").thenReturn(Ability.StaticKeyword.FLYING),
            phrase("For Mirrodin").followedBy("!").thenReturn(Ability.StaticKeyword.FOR_MIRRODIN),
            phrase("Fuse").thenReturn(Ability.StaticKeyword.FUSE),
            phrase("Gravestorm").thenReturn(Ability.TriggeredKeyword.GRAVESTORM),
            phrase("Haste").thenReturn(Ability.StaticKeyword.HASTE),
            phrase("Haunt").thenReturn(Ability.TriggeredKeyword.HAUNT),
            phrase("Hexproof").thenReturn(Ability.StaticKeyword.HEXPROOF),
            phrase("Hidden agenda").thenReturn(Ability.StaticKeyword.HIDDEN_AGENDA),
            phrase("Horsemanship").thenReturn(Ability.StaticKeyword.HORSEMANSHIP),
            phrase("Indestructible").thenReturn(Ability.StaticKeyword.INDESTRUCTIBLE),
            phrase("Infect").thenReturn(Ability.StaticKeyword.INFECT),
            phrase("Ingest").thenReturn(Ability.TriggeredKeyword.INGEST),
            phrase("Intimidate").thenReturn(Ability.StaticKeyword.INTIMIDATE),
            phrase("Lifelink").thenReturn(Ability.StaticKeyword.LIFELINK),
            phrase("Living metal").thenReturn(Ability.StaticKeyword.LIVING_METAL),
            phrase("Living weapon").thenReturn(Ability.TriggeredKeyword.LIVING_WEAPON),
            phrase("Melee").thenReturn(Ability.TriggeredKeyword.MELEE),
            phrase("Menace").thenReturn(Ability.StaticKeyword.MENACE),
            phrase("Mentor").thenReturn(Ability.TriggeredKeyword.MENTOR),
            phrase("Myriad").thenReturn(Ability.TriggeredKeyword.MYRIAD),
            phrase("Nightbound").thenReturn(Ability.TriggeredKeyword.NIGHTBOUND),
            phrase("Partner").thenReturn(Ability.StaticKeyword.PARTNER),
            phrase("Persist").thenReturn(Ability.TriggeredKeyword.PERSIST),
            phrase("Phasing").thenReturn(Ability.StaticKeyword.PHASING),
            phrase("Prowess").thenReturn(Ability.TriggeredKeyword.PROWESS),
            phrase("Reach").thenReturn(Ability.StaticKeyword.REACH),
            phrase("Read ahead").thenReturn(Ability.StaticKeyword.READ_AHEAD),
            phrase("Retrace").thenReturn(Ability.StaticKeyword.RETRACE),
            phrase("Riot").thenReturn(Ability.StaticKeyword.RIOT),
            phrase("Shadow").thenReturn(Ability.StaticKeyword.SHADOW),
            phrase("Shroud").thenReturn(Ability.StaticKeyword.SHROUD),
            phrase("Skulk").thenReturn(Ability.StaticKeyword.SKULK),
            phrase("Solved").thenReturn(Ability.StaticKeyword.SOLVED),
            phrase("Soulbond").thenReturn(Ability.TriggeredKeyword.SOULBOND),
            phrase("Split second").thenReturn(Ability.StaticKeyword.SPLIT_SECOND),
            phrase("Storm").thenReturn(Ability.TriggeredKeyword.STORM),
            phrase("Sunburst").thenReturn(Ability.StaticKeyword.SUNBURST),
            phrase("Training").thenReturn(Ability.TriggeredKeyword.TRAINING),
            phrase("Trample").thenReturn(Ability.StaticKeyword.TRAMPLE),
            phrase("Umbra armor").thenReturn(Ability.StaticKeyword.UMBRA_ARMOR),
            phrase("Undaunted").thenReturn(Ability.StaticKeyword.UNDAUNTED),
            phrase("Undying").thenReturn(Ability.TriggeredKeyword.UNDYING),
            phrase("Vigilance").thenReturn(Ability.StaticKeyword.VIGILANCE),
            phrase("Visit").thenReturn(Ability.TriggeredKeyword.VISIT),
            phrase("Wither").thenReturn(Ability.StaticKeyword.WITHER));

    /// `with no abilities` — the global empty-ability-set predicate.
    private static final Parser<ObjectPropertySelector> NO_ABILITIES =
            phrase("with no abilities").thenReturn(new AbilitySelector.HasNoAbilities());

    /// `with X and Y[, and Z]` — Oxford-comma `and` list, **at
    /// least two** keywords. The size filter is what makes the
    /// outer `anyOf` order work: a bare `with flying` would
    /// otherwise commit here as a singleton list and the
    /// [#OR_COMPOUND] arm below would never get a chance for `with
    /// flying or reach`.
    private static final Parser<ObjectPropertySelector> AND_COMPOUND = phrase("with")
            .then(andList(ABILITY_KEYWORD).suchThat(list -> list.size() >= 2, "and-compound"))
            .map(list -> new ObjectPropertySelector.AllOf(list.stream()
                    .<ObjectPropertySelector>map(AbilitySelector.Has::new)
                    .toList()));

    /// `with X or Y[, or Z]` — Oxford-comma `or` list, **at least
    /// two** keywords. Same singleton-rejection trick as
    /// [#AND_COMPOUND].
    private static final Parser<ObjectPropertySelector> OR_COMPOUND = phrase("with")
            .then(orList(ABILITY_KEYWORD).suchThat(list -> list.size() >= 2, "or-compound"))
            .map(list -> new ObjectPropertySelector.AnyOf(list.stream()
                    .<ObjectPropertySelector>map(AbilitySelector.Has::new)
                    .toList()));

    /// `with X` — single keyword, the catch-all once the compound
    /// arms have rejected singletons.
    private static final Parser<ObjectPropertySelector> SINGLETON =
            phrase("with").then(ABILITY_KEYWORD).map(AbilitySelector.Has::new);

    /// `without X or Y[, or Z]` — negation list, ≥2 keywords.
    /// Combines via [ObjectPropertySelector.AllOf] of
    /// [AbilitySelector.HasNot] per De Morgan:
    /// `NOT(K1 OR K2) = NOT K1 AND NOT K2`. Oracle text only uses
    /// `or` as the coordinator after `without` — there is no
    /// `without X and Y` form in real oracle, so we deliberately
    /// don't accept one (lets a hypothetical occurrence surface as
    /// a parse failure rather than silently producing the same
    /// shape as `or`).
    private static final Parser<ObjectPropertySelector> WITHOUT_OR_COMPOUND = phrase("without")
            .then(orList(ABILITY_KEYWORD).suchThat(list -> list.size() >= 2, "without-or-compound"))
            .map(list -> new ObjectPropertySelector.AllOf(list.stream()
                    .<ObjectPropertySelector>map(AbilitySelector.HasNot::new)
                    .toList()));

    /// `without X` — single keyword negation, the catch-all once
    /// the compound `without` arms have rejected singletons.
    private static final Parser<ObjectPropertySelector> WITHOUT_SINGLETON =
            phrase("without").then(ABILITY_KEYWORD).map(AbilitySelector.HasNot::new);

    /// Top-level entry. Order: [#NO_ABILITIES] first so `with no
    /// abilities` wins over `with` + a hypothetical "no" keyword.
    /// `without` arms before `with` arms is incidental — they don't
    /// share a prefix (`without` ≠ `with`), so order between the
    /// two families doesn't matter.
    public static final Parser<ObjectPropertySelector> ABILITY_SELECTOR =
            anyOf(NO_ABILITIES, WITHOUT_OR_COMPOUND, WITHOUT_SINGLETON, AND_COMPOUND, OR_COMPOUND, SINGLETON);
}
