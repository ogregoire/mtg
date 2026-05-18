package be.imgn.mtg.engine.oracle2.parser.effect;

import static be.imgn.mtg.engine.oracle2.parser.ConditionParser.CONDITION;
import static be.imgn.mtg.engine.oracle2.parser.Parsers.phrase;
import static be.imgn.mtg.engine.oracle2.parser.TypeMatcherParser.SOURCE_MATCHER;
import static be.imgn.mtg.engine.oracle2.parser.TypeMatcherParser.SPELL_MATCHER;
import static com.google.common.labs.parse.Parser.anyOf;
import static com.google.common.labs.parse.Parser.sequence;
import static com.google.common.labs.parse.Parser.string;

import com.google.common.labs.parse.Parser;

import be.imgn.mtg.engine.oracle2.domain.effect.AddManaEffect;
import be.imgn.mtg.engine.oracle2.domain.effect.AddManaEffect.Replacement;
import be.imgn.mtg.engine.oracle2.domain.mana.Mana;
import be.imgn.mtg.engine.oracle2.domain.mana.ProducedMana;
import be.imgn.mtg.engine.oracle2.domain.mana.Restriction;
import be.imgn.mtg.engine.oracle2.parser.ManaParser;

/// Parser for the `Add MANA. […]?` sentence shape. Self-terminating —
/// owns its own period plus the optional follow-up sentences for the
/// baked-in replacement rider and the "Spend this mana only …"
/// restriction.
///
/// Three sentence shapes absorbed in order:
/// 1. **Base** — `Add MANA.` (always present).
/// 2. **Replacement** — `If COND, add MANA' instead.` (optional, River
///    of Tears family).
/// 3. **Restriction** — `Spend this mana only [to <verb-list> | on
///    costs that contain {X}].` (optional, Eldrazi Temple / Cultivator
///    Drone / Omen Hawker / Sage of the Unknowable / Automated
///    Artificer family).
///
/// Replacement is tried before Restriction because both follow-up
/// sentences begin after the base period and their bodies are
/// unambiguous (`If` vs `Spend this mana only`).
public final class AddManaEffectParser {
    private AddManaEffectParser() {}

    /// Literal mana payload — `{G}{G}` → `ProducedMana([Mana({G}), Mana({G})])`.
    /// Each [ManaSymbol][be.imgn.mtg.engine.oracle2.domain.mana.ManaSymbol]
    /// becomes a bare [Mana] (no restriction); the optional
    /// `withRestriction` later distributes a restriction to every
    /// instance. Shared by the base [AddManaEffect] constructor and
    /// the nested [Replacement] constructor.
    private static final Parser<ProducedMana> MANA_PAYLOAD = ManaParser.SYMBOLS.map(
            symbols -> new ProducedMana(symbols.stream().map(Mana::new).toList()));

    // ── Restriction verb clauses ─────────────────────────────────────

    /// "cast \<spell\>" → [Restriction.ToCast].
    private static final Parser<Restriction.ToCast> CAST =
            phrase("cast").then(SPELL_MATCHER).map(Restriction.ToCast::new);

    /// "activate \[an ability|abilities\] \[of \<source\>\]?" →
    /// [Restriction.ToActivateAbility]. The source is `null` when the
    /// "of …" tail is absent.
    private static final Parser<Restriction.ToActivateAbility> ACTIVATE_ABILITY = phrase(
                    "activate [an ability|abilities]")
            .thenReturn(new Restriction.ToActivateAbility(null))
            .optionallyFollowedBy(
                    phrase("of").then(SOURCE_MATCHER), (_, src) -> new Restriction.ToActivateAbility(src));

    /// One verb clause — `cast …` or `activate …`. Covariance widens
    /// to [Restriction] via the explicit type witness.
    private static final Parser<Restriction> VERB_CLAUSE = Parser.<Restriction>anyOf(CAST, ACTIVATE_ABILITY);

    /// "to \<verb-clause\> \[or \<verb-clause\>\]*". Singleton collapses
    /// to the bare arm; multi-element lists wrap in [Restriction.AnyOf].
    private static final Parser<Restriction> TO_BODY = phrase("to")
            .then(VERB_CLAUSE.atLeastOnceDelimitedBy("or"))
            .map(list -> list.size() == 1 ? list.getFirst() : new Restriction.AnyOf(list));

    /// "on costs that contain \<symbol\>" → [Restriction.OnCostsContaining].
    /// Single-symbol payload — multi-symbol bodies aren't attested.
    private static final Parser<Restriction> ON_BODY = phrase("on costs that contain")
            .then(ManaParser.SYMBOL)
            .<Restriction>map(Restriction.OnCostsContaining::new);

    /// "Spend this mana only \[to … | on costs that contain {X}\]."
    /// Owns its terminating period.
    private static final Parser<Restriction> SPEND_ONLY =
            phrase("Spend this mana only").then(anyOf(TO_BODY, ON_BODY)).followedBy(string("."));

    // ── Conditional replacement ──────────────────────────────────────

    /// "If \<condition\>, add \<mana\> instead." — the baked-in
    /// replacement rider on the preceding add. Owns its terminating
    /// period.
    private static final Parser<Replacement> REPLACEMENT = sequence(
            phrase("If").then(CONDITION).followedBy(string(",")),
            phrase("add").then(MANA_PAYLOAD).followedBy(phrase("instead")).followedBy(string(".")),
            Replacement::new);

    // ── Top-level ────────────────────────────────────────────────────

    /// `Add MANA. [If COND, add MANA' instead.]? [Spend this mana
    /// only …]?` — full sentence with both optional riders absorbed
    /// into the same [AddManaEffect].
    public static final Parser<AddManaEffect> ADD_MANA = phrase("Add")
            .then(MANA_PAYLOAD)
            .map(AddManaEffect::new)
            .followedBy(string("."))
            .optionallyFollowedBy(REPLACEMENT, AddManaEffect::withReplacement)
            .optionallyFollowedBy(SPEND_ONLY, AddManaEffect::withRestriction);
}
