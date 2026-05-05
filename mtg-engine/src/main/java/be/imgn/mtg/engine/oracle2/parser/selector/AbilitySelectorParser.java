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
/// for the canonical printed form of each parameter-less keyword.
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
    /// matters when one keyword phrase is a prefix of another:
    /// `double strike` and `first strike` both end in `strike` but
    /// don't actually overlap, so alphabetical works for now —
    /// document longer-prefix-first if a real conflict arises.
    private static final Parser<Ability> ABILITY_KEYWORD = anyOf(
            phrase("banding").thenReturn(Ability.StaticKeyword.BANDING),
            phrase("changeling").thenReturn(Ability.StaticKeyword.CHANGELING),
            phrase("deathtouch").thenReturn(Ability.StaticKeyword.DEATHTOUCH),
            phrase("decayed").thenReturn(Ability.StaticKeyword.DECAYED),
            phrase("defender").thenReturn(Ability.StaticKeyword.DEFENDER),
            phrase("double strike").thenReturn(Ability.StaticKeyword.DOUBLE_STRIKE),
            phrase("fear").thenReturn(Ability.StaticKeyword.FEAR),
            phrase("first strike").thenReturn(Ability.StaticKeyword.FIRST_STRIKE),
            phrase("flash").thenReturn(Ability.StaticKeyword.FLASH),
            phrase("flying").thenReturn(Ability.StaticKeyword.FLYING),
            phrase("haste").thenReturn(Ability.StaticKeyword.HASTE),
            phrase("hexproof").thenReturn(Ability.StaticKeyword.HEXPROOF),
            phrase("horsemanship").thenReturn(Ability.StaticKeyword.HORSEMANSHIP),
            phrase("indestructible").thenReturn(Ability.StaticKeyword.INDESTRUCTIBLE),
            phrase("infect").thenReturn(Ability.StaticKeyword.INFECT),
            phrase("intimidate").thenReturn(Ability.StaticKeyword.INTIMIDATE),
            phrase("lifelink").thenReturn(Ability.StaticKeyword.LIFELINK),
            phrase("menace").thenReturn(Ability.StaticKeyword.MENACE),
            phrase("reach").thenReturn(Ability.StaticKeyword.REACH),
            phrase("shadow").thenReturn(Ability.StaticKeyword.SHADOW),
            phrase("shroud").thenReturn(Ability.StaticKeyword.SHROUD),
            phrase("trample").thenReturn(Ability.StaticKeyword.TRAMPLE),
            phrase("vigilance").thenReturn(Ability.StaticKeyword.VIGILANCE),
            phrase("wither").thenReturn(Ability.StaticKeyword.WITHER),
            phrase("prowess").thenReturn(Ability.TriggeredKeyword.PROWESS));

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
