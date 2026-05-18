package be.imgn.mtg.engine.oracle2.parser;

import static be.imgn.mtg.engine.oracle2.parser.AmountMatcherParser.AMOUNT_MATCHER;
import static be.imgn.mtg.engine.oracle2.parser.Parsers.orList;
import static be.imgn.mtg.engine.oracle2.parser.Parsers.phrase;
import static com.google.common.labs.parse.Parser.anyOf;
import static com.google.common.labs.parse.Parser.or;
import static com.google.common.labs.parse.Parser.string;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import com.google.common.labs.parse.Parser;

import be.imgn.mtg.engine.oracle2.domain.ArtifactType;
import be.imgn.mtg.engine.oracle2.domain.BasicLandType;
import be.imgn.mtg.engine.oracle2.domain.BattleType;
import be.imgn.mtg.engine.oracle2.domain.CardType;
import be.imgn.mtg.engine.oracle2.domain.Color;
import be.imgn.mtg.engine.oracle2.domain.CreatureType;
import be.imgn.mtg.engine.oracle2.domain.EnchantmentType;
import be.imgn.mtg.engine.oracle2.domain.NonBasicLandType;
import be.imgn.mtg.engine.oracle2.domain.Parseable;
import be.imgn.mtg.engine.oracle2.domain.PlaneswalkerType;
import be.imgn.mtg.engine.oracle2.domain.SpellType;
import be.imgn.mtg.engine.oracle2.domain.Subtype;
import be.imgn.mtg.engine.oracle2.domain.Supertype;
import be.imgn.mtg.engine.oracle2.domain.TypeMatcher;

/// Parser for [TypeMatcher] — typed predicate phrases like "creature
/// spell", "colorless Eldrazi", "Chandra planeswalker spell with mana
/// value 4 or greater". Distinct from the
/// [be.imgn.mtg.engine.oracle2.parser.selector.SelectorParser] tree:
/// the output is a pure type/colour/mana-value predicate with no
/// zone, quantifier, or target wrapping.
///
/// Two public entry points reflect where the predicate sits in
/// surrounding oracle text:
///
/// - [#SPELL_MATCHER] — `[a|an]? <pre-atoms>? spell(s) [with <post>]?`
///   then optionally repeated with `or` between whole phrases. Used
///   for the spell argument of `Restriction.ToCast`.
/// - [#SOURCE_MATCHER] — `<pre-atoms>` only (no noun follows). Used
///   for the `of <source>` argument of `Restriction.ToActivateAbility`.
///
/// Atom composition:
/// - Adjacent atoms (space-joined: "colorless Eldrazi") AND-merge
///   into [TypeMatcher.AllOf].
/// - A trailing "with mana value <matcher>" clause AND-merges into
///   the same composite.
/// - Whole-phrase "or" composition produces [TypeMatcher.AnyOf].
/// - Singleton lists stay unwrapped — no `AllOf([single])` or
///   `AnyOf([single])` is ever emitted.
public final class TypeMatcherParser {
    private TypeMatcherParser() {}

    // ── Leaf atoms ───────────────────────────────────────────────────

    /// "Colorless" / "colorless" → [TypeMatcher.Standard#COLORLESS]
    /// (the [Color] enum is the five colours only — colourless is a
    /// separate marker per CR 105.2c).
    private static final Parser<TypeMatcher> COLORLESS = phrase("Colorless").thenReturn(TypeMatcher.Standard.COLORLESS);

    /// "Creature(s)" / "Artifact(s)" / … → [TypeMatcher.IsCardType].
    /// Each [CardType] value's `text()` template is fed through
    /// [Parsers#phrase] for plural inflection and sentence-start
    /// casing.
    private static final Parser<TypeMatcher> CARD_TYPE = Stream.of(CardType.values())
            .map(t -> phrase(t.text()).<TypeMatcher>thenReturn(new TypeMatcher.IsCardType(t)))
            .collect(or());

    /// "Eldrazi" / "Goblin" / "Chandra" / … → [TypeMatcher.IsSubtype].
    /// Pulls every value across all eight subtype enum families.
    private static final Parser<TypeMatcher> SUBTYPE = allSubtypes()
            .map(s -> phrase(s.text()).<TypeMatcher>thenReturn(new TypeMatcher.IsSubtype(s)))
            .collect(or());

    /// "Legendary" / "Basic" / "Snow" / "World" → [TypeMatcher.IsSupertype].
    private static final Parser<TypeMatcher> SUPERTYPE = Stream.of(Supertype.values())
            .map(s -> phrase(s.text()).<TypeMatcher>thenReturn(new TypeMatcher.IsSupertype(s)))
            .collect(or());

    /// "White" / "Blue" / "Black" / "Red" / "Green" → [TypeMatcher.IsColor].
    private static final Parser<TypeMatcher> COLOR = Stream.of(Color.values())
            .map(c -> phrase(c.text()).<TypeMatcher>thenReturn(new TypeMatcher.IsColor(c)))
            .collect(or());

    // ── Negated atoms ────────────────────────────────────────────────

    /// "Noncreature" / "Nonartifact" / … → `Not(IsCardType(...))`.
    private static final Parser<TypeMatcher> NEGATED_CARD_TYPE = Stream.of(CardType.values())
            .map(t -> phrase("Non" + singular(t).toLowerCase(Locale.ROOT))
                    .<TypeMatcher>thenReturn(new TypeMatcher.Not(new TypeMatcher.IsCardType(t))))
            .collect(or());

    /// "Nonlegendary" / "Nonbasic" / "Nonsnow" → `Not(IsSupertype(...))`.
    /// `WORLD` is excluded — "Nonworld" is unattested in oracle text.
    private static final Parser<TypeMatcher> NEGATED_SUPERTYPE = Stream.of(Supertype.values())
            .filter(s -> s != Supertype.WORLD)
            .map(s -> phrase("Non" + singular(s).toLowerCase(Locale.ROOT))
                    .<TypeMatcher>thenReturn(new TypeMatcher.Not(new TypeMatcher.IsSupertype(s))))
            .collect(or());

    /// "non-Goblin" / "non-Eldrazi" / … → `Not(IsSubtype(...))`. The
    /// hyphen separator differs from card-type/supertype negation
    /// (which fuse the prefix); subtype names are capitalized, so a
    /// fused "NonGoblin" would look wrong.
    private static final Parser<TypeMatcher> NEGATED_SUBTYPE =
            string("non-").then(SUBTYPE).map(s -> new TypeMatcher.Not(s));

    /// "Nonwhite" / "Nonblue" / … → `Not(IsColor(...))`.
    private static final Parser<TypeMatcher> NEGATED_COLOR = Stream.of(Color.values())
            .map(c -> phrase("Non" + c.text().toLowerCase(Locale.ROOT))
                    .<TypeMatcher>thenReturn(new TypeMatcher.Not(new TypeMatcher.IsColor(c))))
            .collect(or());

    // ── Atom dispatch — order matters ────────────────────────────────

    /// One pre-atom — single token (or hyphenated `non-X`). Order:
    /// hyphenated subtype-negation first (distinctive `non-` prefix),
    /// then `Non…` fused negations, then `Colorless` (multi-letter
    /// marker), then the bare positive atoms. Subtype is last because
    /// the subtype keyword table is the largest and most likely to
    /// shadow other atoms.
    private static final Parser<TypeMatcher> ATOM = anyOf(
            NEGATED_SUBTYPE,
            NEGATED_CARD_TYPE,
            NEGATED_SUPERTYPE,
            NEGATED_COLOR,
            COLORLESS,
            SUPERTYPE,
            CARD_TYPE,
            COLOR,
            SUBTYPE);

    // ── Composition ──────────────────────────────────────────────────

    /// One or more adjacent pre-atoms, AND-merged. Singleton collapses
    /// to the bare atom (no [TypeMatcher.AllOf] wrapping).
    private static final Parser<TypeMatcher> PRE_ATOMS = ATOM.atLeastOnce().map(TypeMatcherParser::andMerge);

    /// "with mana value <matcher>" → [TypeMatcher.HasManaValue].
    private static final Parser<TypeMatcher> WITH_CLAUSE =
            phrase("with mana value").then(AMOUNT_MATCHER).<TypeMatcher>map(TypeMatcher.HasManaValue::new);

    // ── Public entry points ──────────────────────────────────────────

    /// `[a|an]? <pre-atoms> spell(s) [with mana value <matcher>]?`,
    /// then zero or more `or <same>` whole-phrase alternatives.
    /// Determiner discarded. Pre-atoms are mandatory in this entry
    /// point (bare "spells" with no predicate isn't covered yet — the
    /// `TypeMatcher.Standard.ANYTHING` arm doesn't exist).
    public static final Parser<TypeMatcher> SPELL_MATCHER =
            orList(spellPhrase()).map(TypeMatcherParser::orMerge);

    /// Bare pre-atoms only — used after "of" in the activate-ability
    /// source slot, where no noun follows. AND-merges adjacent atoms.
    public static final Parser<TypeMatcher> SOURCE_MATCHER = PRE_ATOMS;

    // ── Internals ────────────────────────────────────────────────────

    /// One "spell phrase": optional determiner, optional pre-atoms,
    /// "spell(s)" noun, optional `with mana value` suffix. Pre-atoms
    /// default to [TypeMatcher.Standard#ANYTHING] (the AND-identity)
    /// when grammatically absent; [#andCombine] / [#andMerge]
    /// collapse them away.
    private static Parser<TypeMatcher> spellPhrase() {
        return phrase("a(n)")
                .optional()
                .then(PRE_ATOMS.orElse(TypeMatcher.Standard.ANYTHING))
                .followedBy(phrase("spell(s)"))
                .optionallyFollowedBy(WITH_CLAUSE, TypeMatcherParser::andCombine);
    }

    /// AND-merge two matchers — strip ANYTHING (the identity) and
    /// flatten any nested `AllOf`. Singleton results stay unwrapped.
    private static TypeMatcher andCombine(TypeMatcher a, TypeMatcher b) {
        return andMerge(List.of(a, b));
    }

    /// AND-merge a list of matchers — strip [TypeMatcher.Standard#ANYTHING]
    /// (the AND-identity), flatten nested `AllOf`, and collapse
    /// singletons. Empty list after stripping → ANYTHING.
    private static TypeMatcher andMerge(List<TypeMatcher> matchers) {
        var flat = new ArrayList<TypeMatcher>(matchers.size());
        for (var m : matchers) {
            if (m == TypeMatcher.Standard.ANYTHING) continue;
            if (m instanceof TypeMatcher.AllOf all) {
                flat.addAll(all.matchers());
            } else {
                flat.add(m);
            }
        }
        return switch (flat.size()) {
            case 0 -> TypeMatcher.Standard.ANYTHING;
            case 1 -> flat.getFirst();
            default -> new TypeMatcher.AllOf(flat);
        };
    }

    /// OR-merge a list of matchers — singleton collapses; multi-element
    /// lists wrap in [TypeMatcher.AnyOf] (no flattening — `AnyOf` over
    /// `AllOf` arms is the typical shape and shouldn't lose structure).
    private static TypeMatcher orMerge(List<TypeMatcher> matchers) {
        return matchers.size() == 1 ? matchers.getFirst() : new TypeMatcher.AnyOf(matchers);
    }

    /// Strip the `(s)` plural marker / `[Sorcery|Sorceries]` bracket
    /// alternation from a [Parseable]'s `text()` template to recover
    /// the bare singular form (mirrors `TypeSelectorParser#singular`).
    private static String singular(Parseable p) {
        var text = p.text();
        if (text.startsWith("[")) return text.substring(1, text.indexOf('|'));
        var open = text.indexOf('(');
        return open >= 0 ? text.substring(0, open) : text;
    }

    /// Every subtype across the eight enum families.
    private static Stream<Subtype> allSubtypes() {
        return Stream.of(
                        Stream.of(ArtifactType.values()),
                        Stream.of(BattleType.values()),
                        Stream.of(CreatureType.values()),
                        Stream.of(EnchantmentType.values()),
                        Stream.of(BasicLandType.values()),
                        Stream.of(NonBasicLandType.values()),
                        Stream.of(PlaneswalkerType.values()),
                        Stream.of(SpellType.values()))
                .flatMap(s -> s);
    }
}
