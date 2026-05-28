package be.imgn.mtg.engine.oracle2.parser.effect;

import static be.imgn.mtg.engine.oracle2.parser.AmountParser.AMOUNT;
import static be.imgn.mtg.engine.oracle2.parser.AmountParser.FOR_EACH;
import static be.imgn.mtg.engine.oracle2.parser.AmountParser.PROPERTY_OF_AMOUNT;
import static be.imgn.mtg.engine.oracle2.parser.AmountParser.THE_NUMBER_OF;
import static be.imgn.mtg.engine.oracle2.parser.ConditionParser.CONDITION;
import static be.imgn.mtg.engine.oracle2.parser.Parsers.orList;
import static be.imgn.mtg.engine.oracle2.parser.Parsers.phrase;
import static be.imgn.mtg.engine.oracle2.parser.TypeMatcherParser.SOURCE_MATCHER;
import static be.imgn.mtg.engine.oracle2.parser.TypeMatcherParser.SPELL_MATCHER;
import static com.google.common.labs.parse.Parser.anyOf;
import static com.google.common.labs.parse.Parser.one;
import static com.google.common.labs.parse.Parser.sequence;
import static com.google.common.labs.parse.Parser.string;
import static com.google.common.labs.parse.Parser.word;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.google.common.labs.parse.Parser;

import be.imgn.mtg.engine.oracle2.domain.Amount;
import be.imgn.mtg.engine.oracle2.domain.effect.AddManaEffect;
import be.imgn.mtg.engine.oracle2.domain.effect.AddManaEffect.Replacement;
import be.imgn.mtg.engine.oracle2.domain.mana.Mana;
import be.imgn.mtg.engine.oracle2.domain.mana.ManaSymbol;
import be.imgn.mtg.engine.oracle2.domain.mana.ManaType;
import be.imgn.mtg.engine.oracle2.domain.mana.Palette;
import be.imgn.mtg.engine.oracle2.domain.mana.ProducedMana;
import be.imgn.mtg.engine.oracle2.domain.mana.Restriction;
import be.imgn.mtg.engine.oracle2.domain.selector.ObjectSelector;
import be.imgn.mtg.engine.oracle2.parser.ManaParser;
import be.imgn.mtg.engine.oracle2.parser.selector.ObjectSelectorParser;
import be.imgn.mtg.engine.oracle2.parser.selector.SelectorParser;

/// Parser for the `Add MANA. […]?` sentence shape. Self-terminating —
/// owns its own period plus the optional follow-up sentences for the
/// baked-in replacement rider and the "Spend this mana only …"
/// restriction.
///
/// Surface shapes covered:
/// 1. **Bare imperative** — `Add MANA.` (implicit "you" actor).
/// 2. **Actor form** — `<player> add(s) MANA.` (Tangleroot).
/// 3. **Replacement** — `If COND, add MANA' instead.` (River of Tears
///    family).
/// 4. **Restriction** — `Spend this mana only [to <verb-list> | on
///    costs that contain {X}].`
/// 5. **Trailing flavour** — "\[they|you\] choose" and "where X is …"
///    absorbed and discarded.
///
/// Replacement is tried before Restriction because both follow-up
/// sentences begin after the base period and their bodies are
/// unambiguous (`If` vs `Spend this mana only`).
public final class AddManaEffectParser {
    private AddManaEffectParser() {}

    // ── Palettes ─────────────────────────────────────────────────────

    /// The five basic colours as an explicit palette — the default
    /// palette for every described mana shape ("one mana of any
    /// colour", "three mana of any one colour", "three mana of
    /// different colours", "three mana in any combination of colours").
    private static final Palette BASIC_COLORS_PALETTE = new Palette.Explicit(List.of(
            ManaSymbol.Colored.of(ManaType.WHITE),
            ManaSymbol.Colored.of(ManaType.BLUE),
            ManaSymbol.Colored.of(ManaType.BLACK),
            ManaSymbol.Colored.of(ManaType.RED),
            ManaSymbol.Colored.of(ManaType.GREEN)));

    /// "that \[a|an|the\]? \<source\>" — the relative-clause
    /// introducer of a dynamic palette. Defers to
    /// [ObjectSelectorParser#OBJECT_SELECTOR] for the source
    /// selector so the result stays narrow [ObjectSelector] (no
    /// `QuantifierSelector` wrapper — the count is implicit in the
    /// palette role, not part of the AST). Strips the optional
    /// determiner up front because oracle text writes "that a land
    /// you control" / "that the sacrificed land" / bare "that land".
    /// No comma-absorption risk because the immediate follow-up is
    /// a word phrase ("could produce" / "produced").
    ///
    /// **Scope note**: bare "that land" parses as the generic object
    /// selector "land" (any land) rather than the demonstrative
    /// back-reference oracle uses for Mirari's Wake; that gap will
    /// close when oracle2 grows a back-reference `ObjectSelector` arm.
    private static final Parser<ObjectSelector> THAT_LAND_SOURCE =
            phrase("that").then(phrase("[a|an|the]").optional().then(ObjectSelectorParser.OBJECT_SELECTOR));

    /// "any \[color|type\]" → [Palette.Filter]. Captures the rule
    /// 106.7 colour-vs-type discriminator.
    private static final Parser<Palette.Filter> ANY_COLOR_OR_TYPE = phrase("any")
            .then(anyOf(
                    word("color").thenReturn(Palette.Filter.COLOR), word("type").thenReturn(Palette.Filter.TYPE)));

    /// Tail of `that \<source\> \[could produce|produced\]` — emits a
    /// [Palette] given the parsed source and the previously-captured
    /// filter.
    private static Parser<Palette> producedPaletteTail(ObjectSelector source, Palette.Filter filter) {
        return anyOf(
                phrase("could produce").<Palette>thenReturn(new Palette.CouldProduce(source, filter)),
                phrase("produced").<Palette>thenReturn(new Palette.Produced(source, filter)));
    }

    // ── Literal mana sequences ───────────────────────────────────────

    /// One literal mana sequence — `{G}{G}` → `Exact([Mana({G}),
    /// Mana({G})])`. Each [ManaSymbol][be.imgn.mtg.engine.oracle2.domain.mana.ManaSymbol]
    /// becomes a bare [Mana] (no restriction); a later
    /// `withRestriction` distributes a restriction to every instance.
    private static final Parser<ProducedMana.Exact> MANA_SEQUENCE = ManaParser.SYMBOLS.map(
            symbols -> new ProducedMana.Exact(symbols.stream().map(Mana::new).toList()));

    /// "An additional" prefix that some "Add …" sentences carry
    /// (Caged Sun: "an additional one mana of that colour"). Flavour;
    /// consumed and discarded so the rest of the mana phrase parses
    /// uniformly.
    private static final Parser<?> ADDITIONAL = phrase("an additional");

    // ── "One mana of any …" arms (static + dynamic palettes) ─────────

    /// `One mana of any [color|type] that <land> [could produce|produced]`
    /// — Reflecting Pool / Star Compass / Mirari's Wake family.
    private static final Parser<ProducedMana> ONE_MANA_DYNAMIC = sequence(
                    phrase("One mana of").then(ANY_COLOR_OR_TYPE), THAT_LAND_SOURCE, Map::entry)
            .flatMap(fs -> producedPaletteTail(fs.getValue(), fs.getKey()))
            .<ProducedMana>map(palette -> new ProducedMana.OfOneColor(new Amount.Exact(1), palette));

    /// `One mana of any color` — bare static fallback. Order: tried
    /// after [#ONE_MANA_DYNAMIC] so the dynamic-tailed variants win
    /// when their tail is present.
    private static final Parser<ProducedMana> ONE_MANA_STATIC = phrase("One mana of any color")
            .<ProducedMana>thenReturn(new ProducedMana.OfOneColor(new Amount.Exact(1), BASIC_COLORS_PALETTE));

    // ── AMOUNT-led described shapes ──────────────────────────────────

    /// Tail dispatch keyed off the AMOUNT that precedes every
    /// described-mana shape (`AMOUNT mana of any one color`, `AMOUNT
    /// mana of [that|the chosen] color`, `AMOUNT mana of different
    /// colors`, `AMOUNT mana in any combination of colors`, `AMOUNT
    /// <SYMBOL>`). The AMOUNT is parsed once at the outer
    /// [#AMOUNT_LED] level and each tail returns a `Function<Amount,
    /// ProducedMana>` builder so we don't re-parse the leading count.
    private static final Parser<Function<Amount, ProducedMana>> DESCRIBED_TAIL = anyOf(
            phrase("mana of any one color")
                    .<Function<Amount, ProducedMana>>thenReturn(
                            amt -> new ProducedMana.OfOneColor(amt, BASIC_COLORS_PALETTE)),
            phrase("mana of [that|the chosen] color")
                    .<Function<Amount, ProducedMana>>thenReturn(ProducedMana.OfThatColor::new),
            phrase("mana of different colors")
                    .<Function<Amount, ProducedMana>>thenReturn(
                            amt -> new ProducedMana.OfDistinctColors(amt, BASIC_COLORS_PALETTE)),
            phrase("mana in any combination of colors")
                    .<Function<Amount, ProducedMana>>thenReturn(
                            amt -> new ProducedMana.Mixed(amt, BASIC_COLORS_PALETTE)),
            ManaParser.SYMBOL.<Function<Amount, ProducedMana>>map(
                    sym -> amt -> new ProducedMana.Repeated(amt, List.of(sym))));

    /// `AMOUNT TAIL` — described-count mana shapes. AMOUNT is parsed
    /// once; the tail dispatches into [ProducedMana.OfOneColor] /
    /// [ProducedMana.OfThatColor] / [ProducedMana.OfDistinctColors] /
    /// [ProducedMana.Mixed] / [ProducedMana.Repeated].
    private static final Parser<ProducedMana> AMOUNT_LED = sequence(AMOUNT, DESCRIBED_TAIL, (amt, fn) -> fn.apply(amt));

    // ── Repeated shapes (count is described, not literal) ────────────

    /// `<SYMBOLS> for each <selector>` — Mana Seism-style for-each
    /// scaling. The count is an [Amount.CountOf] over the selector.
    private static final Parser<ProducedMana> REPEATED_FOR_EACH =
            sequence(ManaParser.SYMBOLS, FOR_EACH, (syms, count) -> new ProducedMana.Repeated(count, syms));

    /// `an amount of <SYMBOL> equal to <subject>'s <property>` —
    /// Viridian Joiner: `an amount of {G} equal to this creature's
    /// power`.
    private static final Parser<ProducedMana> AMOUNT_OF_SYMBOL_EQUAL_TO = sequence(
            phrase("an amount of").then(ManaParser.SYMBOL),
            PROPERTY_OF_AMOUNT,
            (sym, amt) -> new ProducedMana.Repeated(amt, List.of(sym)));

    // ── Literal-or-alternative payload (Exact / OneOf) ───────────────

    /// Literal-or-alternative fallback — `MANA_SEQUENCE (or MANA_SEQUENCE)*`.
    /// Singleton collapses to the bare [ProducedMana.Exact];
    /// multi-element lists wrap in [ProducedMana.OneOf].
    private static final Parser<ProducedMana> LITERAL_PAYLOAD = orList(MANA_SEQUENCE)
            .map(seqs -> seqs.size() == 1 ? seqs.getFirst() : new ProducedMana.OneOf(List.copyOf(seqs)));

    // ── Top-level MANA_PAYLOAD ───────────────────────────────────────

    /// Full mana payload. Order matters (dot-parse is non-backtracking
    /// within sequences, but `anyOf` rolls back at arm boundaries):
    /// 1. [#REPEATED_FOR_EACH] / [#AMOUNT_OF_SYMBOL_EQUAL_TO] — start
    ///    with mana symbols + a distinctive tail (`for each` / `an
    ///    amount of`); must precede [#LITERAL_PAYLOAD] since both
    ///    start with mana symbols.
    /// 2. [#ONE_MANA_DYNAMIC] / [#ONE_MANA_STATIC] — `One mana of
    ///    any color [palette]?`; dynamic arm first so its tail wins
    ///    when present.
    /// 3. [#AMOUNT_LED] — every other described-count shape (shared
    ///    AMOUNT prefix, dispatch on tail).
    /// 4. [#LITERAL_PAYLOAD] — literal `{X}{Y}` sequences and `X or Y`
    ///    alternatives (fallback).
    private static final Parser<ProducedMana> MANA_PAYLOAD = anyOf(
            AMOUNT_OF_SYMBOL_EQUAL_TO,
            REPEATED_FOR_EACH,
            ONE_MANA_DYNAMIC,
            ONE_MANA_STATIC,
            AMOUNT_LED,
            LITERAL_PAYLOAD);

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
    /// to the bare arm; multi-element lists wrap in [Restriction.OneOf].
    private static final Parser<Restriction> TO_BODY = phrase("to")
            .then(VERB_CLAUSE.atLeastOnceDelimitedBy("or"))
            .map(list -> list.size() == 1 ? list.getFirst() : new Restriction.OneOf(list));

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

    /// Trailing "\[they|you\] choose" flavour (Spectral Searchlight)
    /// — restates the chooser of a [ProducedMana.OfOneColor] /
    /// [ProducedMana.Mixed] shape. Consumed and discarded; the
    /// chooser is implied by [AddManaEffect#player] (or the resolving
    /// controller when absent).
    private static final Parser<?> TRAILING_CHOOSER = phrase("[they|you] choose");

    /// Trailing ", where X is \<amount\>" — binds the `X` referenced
    /// in the produced-mana payload to a structured [Amount]. Today
    /// the right-hand side parses as [AmountParser#THE_NUMBER_OF]
    /// ("the number of \<selector\>") or any plain [AmountParser#AMOUNT]
    /// atom. Anything outside those shapes fails the parse rather
    /// than being silently swallowed.
    private static final Parser<Amount> WHERE_X_IS =
            string(",").then(phrase("where X is")).then(anyOf(THE_NUMBER_OF, AMOUNT));

    /// Common body for bare and actor forms: `MANA_PAYLOAD [trailing
    /// flavour]? [, where X is \<amount\>]?` — the parts after the
    /// leading "Add" / "\<player\> add(s)" and before the terminating
    /// period. The optional X-binding is captured as
    /// [AddManaEffect#xDefinition].
    private static Parser<AddManaEffect> bodyAfterPayload(Parser<AddManaEffect> base) {
        return base.optionallyFollowedBy(TRAILING_CHOOSER, (am, _) -> am)
                .optionallyFollowedBy(WHERE_X_IS, AddManaEffect::withXDefinition);
    }

    /// `Add [an additional]? MANA [trailing flavour]?.` — base form,
    /// implicit "you" actor.
    private static final Parser<AddManaEffect> BARE_ADD_MANA = bodyAfterPayload(phrase("Add")
                    .optionallyFollowedBy(ADDITIONAL, (s, _) -> s)
                    .then(MANA_PAYLOAD)
                    .map(AddManaEffect::new))
            .followedBy(one('.'));

    /// `<player> add(s) [an additional]? MANA [trailing flavour]?.` —
    /// actor form (Tangleroot, "Target player adds {G}", "Each
    /// opponent adds {C}"). Uses [SelectorParser#SELECTOR] (not the
    /// narrower [be.imgn.mtg.engine.oracle2.parser.selector.PlayerSelectorParser#PLAYER_SELECTOR])
    /// to pick up "target X" and "each X" quantifier wrappers that the
    /// player-only parser doesn't model. No `PropertyParser`
    /// comma-absorption risk here because the verb "add(s)"
    /// terminates the player phrase.
    private static final Parser<AddManaEffect> ACTOR_ADD_MANA = bodyAfterPayload(sequence(
                    SelectorParser.SELECTOR.followedBy(phrase("add(s)")).optionallyFollowedBy(ADDITIONAL, (p, _) -> p),
                    MANA_PAYLOAD,
                    (player, payload) -> new AddManaEffect(payload).withPlayer(player)))
            .followedBy(one('.'));

    /// Full add-mana sentence — actor form tried first (distinctive
    /// player-led prefix), then bare imperative; both share the same
    /// trailing replacement / restriction rider absorption.
    public static final Parser<AddManaEffect> ADD_MANA = anyOf(ACTOR_ADD_MANA, BARE_ADD_MANA)
            .optionallyFollowedBy(REPLACEMENT, AddManaEffect::withReplacement)
            .optionallyFollowedBy(SPEND_ONLY, AddManaEffect::withRestriction);
}
