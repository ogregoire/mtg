package be.imgn.mtg.engine.oracle2.parser.selector;

import static be.imgn.mtg.engine.oracle2.parser.Parsers.phrase;
import static com.google.common.labs.parse.Parser.anyOf;
import static com.google.common.labs.parse.Parser.or;
import static com.google.common.labs.parse.Parser.string;

import java.util.stream.Stream;

import com.google.common.labs.parse.Parser;

import be.imgn.mtg.engine.oracle2.domain.ArtifactType;
import be.imgn.mtg.engine.oracle2.domain.BasicLandType;
import be.imgn.mtg.engine.oracle2.domain.BattleType;
import be.imgn.mtg.engine.oracle2.domain.CardType;
import be.imgn.mtg.engine.oracle2.domain.CreatureType;
import be.imgn.mtg.engine.oracle2.domain.EnchantmentType;
import be.imgn.mtg.engine.oracle2.domain.NonBasicLandType;
import be.imgn.mtg.engine.oracle2.domain.Parseable;
import be.imgn.mtg.engine.oracle2.domain.PlaneswalkerType;
import be.imgn.mtg.engine.oracle2.domain.SpellType;
import be.imgn.mtg.engine.oracle2.domain.Subtype;
import be.imgn.mtg.engine.oracle2.domain.Supertype;
import be.imgn.mtg.engine.oracle2.domain.selector.CardTypeSelector;
import be.imgn.mtg.engine.oracle2.domain.selector.SubtypeSelector;
import be.imgn.mtg.engine.oracle2.domain.selector.SupertypeSelector;

/// Parsers for the three type-axis [characteristic][be.imgn.mtg.engine.oracle2.domain.selector.CharacteristicSelector]
/// selectors that share the same shape: enumerate every value of the
/// underlying enum, build a `phrase()` matcher from each value's
/// [be.imgn.mtg.engine.oracle2.domain.Parseable#text()], and dispatch
/// over the union.
///
/// Negation (`Noncreature`, `non-Human`, `Nonbasic`) lives here too:
/// each axis has its own [CardTypeSelector.IsNot] / [SupertypeSelector.IsNot]
/// / [SubtypeSelector.IsNot] arm, and the per-axis dispatch tries the
/// negated form before the positive form so the longer prefix wins.
public final class TypeSelectorParser {
    private TypeSelectorParser() {}

    // ── CardType ───────────────────────────────────────────────────

    /// [CardTypeSelector.Is] — "creature", "creatures", "Sorceries",
    /// etc. Each [CardType] value's `text()` template is fed through
    /// [be.imgn.mtg.engine.oracle2.parser.Parsers#phrase] to handle
    /// plural inflection and sentence-start casing.
    private static final Parser<CardTypeSelector.Is> CARD_TYPE_IS = Stream.of(CardType.values())
            .map(t -> phrase(t.text()).thenReturn(new CardTypeSelector.Is(t)))
            .collect(or());

    /// [CardTypeSelector.IsNot] — "Noncreature", "Nonland",
    /// "Nonsorcery", etc. One arm per [CardType] value, built from
    /// `"Non" + singular(t).toLowerCase()`. The [#singular] helper
    /// strips the `(s)` plural marker and the `[Sorcery|Sorceries]`
    /// bracket alternation so we get the bare singular word.
    private static final Parser<CardTypeSelector.IsNot> CARD_TYPE_IS_NOT = Stream.of(CardType.values())
            .map(t -> phrase("Non" + singular(t).toLowerCase()).thenReturn(t))
            .collect(or())
            .map(CardTypeSelector.IsNot::new);

    /// "shares a card type with X" — [CardTypeSelector.SharesACardTypeWith].
    private static final Parser<CardTypeSelector> CARD_TYPE_SHARES_WITH =
            phrase("shares a card type with").then(Refs.OBJECT_SELECTOR).map(CardTypeSelector.SharesACardTypeWith::new);

    /// Top-level [CardTypeSelector]. Order: `SharesACardTypeWith`
    /// first (longest, most-specific multi-word prefix), then
    /// negated `IsNot` (`Noncreature` before `Creature`), then `Is`.
    public static final Parser<CardTypeSelector> CARD_TYPE_SELECTOR =
            anyOf(CARD_TYPE_SHARES_WITH, CARD_TYPE_IS_NOT, CARD_TYPE_IS);

    // ── Supertype ──────────────────────────────────────────────────

    /// [SupertypeSelector.Is] — "legendary", "basic", "snow", "world".
    private static final Parser<SupertypeSelector.Is> SUPERTYPE_IS = Stream.of(Supertype.values())
            .map(s -> phrase(s.text()).thenReturn(new SupertypeSelector.Is(s)))
            .collect(or());

    /// [SupertypeSelector.IsNot] — `Non{supertype}`. One arm per
    /// [Supertype] value except [Supertype#WORLD], which has no
    /// negated oracle phrasing ("Nonworld" is unattested).
    private static final Parser<SupertypeSelector.IsNot> SUPERTYPE_IS_NOT = Stream.of(Supertype.values())
            .filter(s -> s != Supertype.WORLD)
            .map(s -> phrase("Non" + singular(s).toLowerCase()).thenReturn(s))
            .collect(or())
            .map(SupertypeSelector.IsNot::new);

    /// Top-level [SupertypeSelector]. Negated form first so "Nonbasic"
    /// wins over "Basic".
    public static final Parser<SupertypeSelector> SUPERTYPE_SELECTOR = anyOf(SUPERTYPE_IS_NOT, SUPERTYPE_IS);

    // ── Subtype ────────────────────────────────────────────────────

    /// [SubtypeSelector.Is] — every subtype across all 8 enum families
    /// ([CreatureType], [ArtifactType], [EnchantmentType], [BattleType],
    /// [BasicLandType], [NonBasicLandType], [PlaneswalkerType],
    /// [SpellType]).
    public static final Parser<SubtypeSelector.Is> SUBTYPE_IS = allSubtypes()
            .map(s -> phrase(s.text()).thenReturn(new SubtypeSelector.Is(s)))
            .collect(or());

    /// [SubtypeSelector.IsNot] — "non-Human", "non-Goblin", … The
    /// hyphen is not consumed as whitespace, so the subtype parser
    /// runs immediately after `string("non-")`.
    private static final Parser<SubtypeSelector.IsNot> SUBTYPE_IS_NOT =
            string("non-").then(SUBTYPE_IS).map(is -> new SubtypeSelector.IsNot(is.subtype()));

    /// "shares a creature type with X" — [SubtypeSelector.SharesACreatureTypeWith].
    private static final Parser<SubtypeSelector> SUBTYPE_SHARES_WITH = phrase("shares a creature type with")
            .then(Refs.OBJECT_SELECTOR)
            .map(SubtypeSelector.SharesACreatureTypeWith::new);

    /// Top-level [SubtypeSelector]. Order: `SharesACreatureTypeWith`,
    /// then `IsNot` (hyphen-prefixed), then positive `Is`.
    public static final Parser<SubtypeSelector> SUBTYPE_SELECTOR =
            anyOf(SUBTYPE_SHARES_WITH, SUBTYPE_IS_NOT, SUBTYPE_IS);

    /// Extracts the canonical singular form from a [Parseable]'s
    /// `text()` template. Strips the `(s)` plural marker
    /// (`"Creature(s)"` → `"Creature"`) and the bracket alternation
    /// (`"[Sorcery|Sorceries]"` → `"Sorcery"`). Plain templates
    /// (`"Legendary"`, `"White"`) pass through unchanged.
    private static String singular(Parseable p) {
        var text = p.text();
        if (text.startsWith("[")) return text.substring(1, text.indexOf('|'));
        var open = text.indexOf('(');
        return open >= 0 ? text.substring(0, open) : text;
    }

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
