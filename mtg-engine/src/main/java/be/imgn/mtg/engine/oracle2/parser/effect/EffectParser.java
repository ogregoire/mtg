package be.imgn.mtg.engine.oracle2.parser.effect;

import static be.imgn.mtg.engine.oracle2.parser.Parsers.andList;
import static be.imgn.mtg.engine.oracle2.parser.Parsers.phrase;
import static com.google.common.labs.parse.Parser.anyOf;
import static com.google.common.labs.parse.Parser.sequence;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import com.google.common.labs.parse.Parser;

import be.imgn.mtg.engine.oracle2.domain.Amount;
import be.imgn.mtg.engine.oracle2.domain.effect.ChoiceEffect;
import be.imgn.mtg.engine.oracle2.domain.effect.Effect;
import be.imgn.mtg.engine.oracle2.domain.effect.SharedSubjectEffect;
import be.imgn.mtg.engine.oracle2.domain.selector.ObjectSelector;
import be.imgn.mtg.engine.oracle2.domain.selector.PlayerRelationSelector;
import be.imgn.mtg.engine.oracle2.domain.selector.PlayerSelector;
import be.imgn.mtg.engine.oracle2.domain.selector.QuantifierSelector;
import be.imgn.mtg.engine.oracle2.domain.selector.Selector;
import be.imgn.mtg.engine.oracle2.parser.selector.SelectorParser;

/// Top-level entry for the oracle2 effect grammar.
///
/// Two surface shapes:
///
/// 1. **Verb-first**: "Destroy SELECTOR.", "Exile SELECTOR." — the
///    verb is the first word and operates on the rest of the sentence
///    as its argument, with no player-side subject at all.
/// 2. **Subject-led**: `[SUBJECT]? verb …` — an optional leading
///    [Selector] subject followed by one or more verb clauses joined
///    by "and". When the subject is omitted ("Draw a card.", "Scry
///    1.", "Sacrifice a creature."), [#IMPLICIT_YOU] fills in so the
///    AST for an implicit-subject sentence is identical to its
///    explicit-"you" sibling ("You draw a card."). A single verb
///    collapses to the bare effect with the subject inlined; two or
///    more verbs fan out into a [SharedSubjectEffect] with the
///    subject captured exactly once and each clause referencing it
///    via the matching per-axis sentinel
///    ([PlayerSelector.Bound] / [ObjectSelector.Bound]).
///    This makes "one target chosen, used by both clauses" (CR 115.1)
///    a structural property of the AST.
///
/// **Per-verb convention** — every subject-led verb clause is a
/// `public static final Parser<Function<Selector, Effect>>` named
/// `<VERB>_FN`, built via [#subjectVerb], and registered in
/// [#SUBJECT_VERB]'s `anyOf`. The signature of `subjectVerb`
/// (constructor takes `(Selector, Arg)` → specific Effect arm) makes
/// the (subject, arg) ordering impossible to swap by accident.
public final class EffectParser {
    private EffectParser() {}

    /// All subject-led verb clauses. Each emits a function from the
    /// shared subject to the full effect.
    private static final Parser<Function<Selector, Effect>> SUBJECT_VERB = anyOf(
            DrawEffectParser.DRAWS_FN,
            DiscardEffectParser.DISCARDS_FN,
            MillEffectParser.MILLS_FN,
            SacrificeEffectParser.SACRIFICES_FN,
            ShuffleEffectParser.SHUFFLES_FN,
            SkipStepEffectParser.SKIPS_STEP_FN,
            LookAtEffectParser.LOOKS_AT_FN,
            DamageEffectParser.DEALS_DAMAGE_FN,
            PlayWithHandsRevealedEffectParser.PLAY_HANDS_REVEALED_FN,
            TakeExtraTurnEffectParser.TAKE_EXTRA_TURN_FN,
            GainLifeEffectParser.GAINS_LIFE_FN,
            LoseLifeEffectParser.LOSES_LIFE_FN,
            ScryEffectParser.SCRIES_FN,
            // Cant* arms — "be blocked" must precede "block" so the
            // longer prefix wins.
            CantEffectParser.CANT_BE_BLOCKED_FN,
            CantEffectParser.CANT_BE_COUNTERED_FN,
            // "can't X or Y" composition first — its ≥2 size guard
            // rejects singletons, so the per-verb arms still match
            // the single-suffix case.
            CantEffectParser.CANT_COMPOUND_FN,
            // "alone" variants before bare attack/block — longer
            // prefix must win.
            CantEffectParser.CANT_ATTACK_ALONE_FN,
            CantEffectParser.CANT_ATTACK_FN,
            CantEffectParser.CANT_BLOCK_ALONE_FN,
            CantEffectParser.CANT_BLOCK_FN,
            CantEffectParser.CANT_CYCLE_FN,
            CantEffectParser.CANT_SEARCH_LIBRARY_FN,
            CantEffectParser.CANT_CAST_FN,
            CantEffectParser.MUST_BE_BLOCKED_FN);

    /// All verb-first verb constructors — one
    /// `Parser<Function<Selector, Effect>>` per Effect arm whose
    /// surface form is `\<Verb\> SELECTOR`. Reused by both the
    /// singleton dispatch in [#VERB_FIRST_SINGLE] and the generic
    /// "Verb or Verb \<target\>" composition in [#VERB_CHOICE].
    /// Adding a new verb-first effect = add its `*_VERB` constructor
    /// here and its singleton parser in [#VERB_FIRST_SINGLE].
    private static final Parser<Function<Selector, ? extends Effect>> VERB_FIRST_VERB = anyOf(
            CounterEffectParser.COUNTER_VERB,
            DestroyEffectParser.DESTROY_VERB,
            ExileEffectParser.EXILE_VERB,
            RegenerateEffectParser.REGENERATE_VERB,
            TapEffectParser.TAP_VERB,
            TransformEffectParser.TRANSFORM_VERB,
            UntapEffectParser.UNTAP_VERB);

    /// "\<Verb\> or \<Verb\> \[or \<Verb\>…\] \<target\>" — generic
    /// distributed-target choice across any two or more verb-first
    /// verbs. The shared target is materialised into each
    /// alternative; the alternatives are collected into a
    /// [ChoiceEffect] so the "controller picks one at resolution"
    /// semantics (CR 700.2a) are explicit in the AST. Examples:
    /// "Tap or untap target creature.", "Untap or tap target
    /// permanent.", "Destroy or exile target creature.". The size
    /// guard keeps singleton "Tap target …" / "Destroy target …"
    /// matches from being stolen here — they fall through to
    /// [#VERB_FIRST_SINGLE].
    private static final Parser<Effect> VERB_CHOICE = sequence(
            VERB_FIRST_VERB.atLeastOnceDelimitedBy("or").suchThat(l -> l.size() >= 2, "verb-first verb chain (≥2)"),
            SelectorParser.SELECTOR,
            (verbs, subject) -> {
                Selector placeholder = pickPlaceholder(subject);
                return new ChoiceEffect(
                        subject,
                        verbs.stream().<Effect>map(v -> v.apply(placeholder)).toList());
            });

    /// Singleton verb-first parsers — `\<Verb\> SELECTOR` for one
    /// verb. Falls through from [#VERB_CHOICE] when only one verb is
    /// present.
    private static final Parser<Effect> VERB_FIRST_SINGLE = anyOf(
            AddCounterEffectParser.ADD_COUNTER,
            CounterEffectParser.COUNTER,
            DestroyEffectParser.DESTROY,
            ExileEffectParser.EXILE,
            FlipEffectParser.FLIP,
            LegendRuleEffectParser.LEGEND_RULE,
            MayPlayLandFromZoneEffectParser.MAY_PLAY_LAND_FROM_ZONE,
            RegenerateEffectParser.REGENERATE,
            RingTemptsEffectParser.RING_TEMPTS,
            SetLifeTotalEffectParser.SET_LIFE_TOTAL,
            TapEffectParser.TAP,
            TransformEffectParser.TRANSFORM,
            UntapEffectParser.UNTAP);

    /// Verb-first imperatives that have no implicit-subject reading —
    /// the verb's subject is always the game itself, not "you". Bare
    /// imperatives whose subject is the resolving player ("Draw a
    /// card.", "Scry 1.", "Sacrifice a creature.") instead fall
    /// through to [#SUBJECT_LED] and pick up [#IMPLICIT_YOU].
    ///
    /// Order: [#VERB_CHOICE] first (the longer-prefix multi-verb
    /// match wins); then [#VERB_FIRST_SINGLE] for the bare one-verb
    /// case.
    private static final Parser<Effect> VERB_FIRST = anyOf(VERB_CHOICE, VERB_FIRST_SINGLE);

    /// Default subject for omitted-subject imperatives — "you" wrapped
    /// in the canonical `Quantifier(Exact(1), …)` so an implicit "you"
    /// is structurally indistinguishable from an explicit "you" as
    /// parsed by [SelectorParser]. Keeps the AST for "Draw a card."
    /// and "You draw a card." aligned.
    private static final Selector IMPLICIT_YOU =
            new QuantifierSelector(new Amount.Exact(1), PlayerRelationSelector.YOU);

    /// `verb1, then verb2` — temporal-sequence connector. Tried before
    /// the standard `and`-list so the comma before `then` doesn't get
    /// stolen by a bare singleton match. Deliberate / Introduction
    /// to Prophecy: "Scry 2, then draw a card.".
    private static final Parser<List<Function<Selector, Effect>>> SUBJECT_VERB_THEN_LIST =
            sequence(SUBJECT_VERB.followedBy(phrase(",")), phrase("then").then(SUBJECT_VERB), List::of);

    /// A SUBJECT_VERB clause that tolerates an optional leading
    /// "you" — Succumb to Temptation: "You draw two cards and you
    /// lose 2 life." (the second clause repeats the subject for
    /// English flow; semantically identical to "and lose 2 life").
    /// The "you" is consumed and discarded; the outer SUBJECT_LED
    /// fans the bound subject across both clauses.
    private static final Parser<Function<Selector, Effect>> SUBJECT_VERB_WITH_OPTIONAL_YOU =
            anyOf(phrase("you").then(SUBJECT_VERB), SUBJECT_VERB);

    /// `[SUBJECT]? verb1 X [, then verb2 Y | [, verb2 Y[, and verb3 Z]]].`
    /// — distributes the subject across each verb clause. The leading
    /// subject is optional: when absent, [#IMPLICIT_YOU] is used
    /// (covers bare "Draw a card.", "Scry 1.", "Sacrifice a
    /// creature."). Single verb returns the bare effect; multi-verb
    /// wraps in [SharedSubjectEffect]. The `, then` arm precedes the
    /// `and`-list because the comma before `then` would otherwise be
    /// left dangling for the outer sentence parser.
    private static final Parser<Effect> SUBJECT_LED = sequence(
            SelectorParser.SELECTOR.orElse(IMPLICIT_YOU),
            anyOf(SUBJECT_VERB_THEN_LIST, andList(SUBJECT_VERB_WITH_OPTIONAL_YOU)),
            EffectParser::distribute);

    /// Single sentence, terminating period required. Covers
    /// [#VERB_FIRST] (verb-led imperatives) and [#SUBJECT_LED]
    /// (subject + verb-list); both share the same "one period at the
    /// end" convention.
    private static final Parser<Effect> SENTENCE_EFFECT =
            anyOf(VERB_FIRST, SUBJECT_LED).followedBy(phrase("."));

    /// One effect. Dispatches between:
    /// - [AddManaEffectParser#ADD_MANA] — self-terminating "Add MANA.
    ///   \[…\]?" that owns its own period plus optional replacement /
    ///   restriction follow-up sentences. Tried first because its
    ///   leading `Add` is unambiguous.
    /// - [#SENTENCE_EFFECT] — every other single-sentence effect.
    public static final Parser<Effect> EFFECT = Parser.<Effect>anyOf(AddManaEffectParser.ADD_MANA, SENTENCE_EFFECT);

    /// Canonical shape for a subject-led verb clause. Use this for
    /// every new `*_FN` constant — the resulting parser slots into
    /// [#SUBJECT_VERB] without further wiring, and the binary
    /// constructor signature makes the (subject, arg) ordering
    /// impossible to swap by accident.
    public static <A, E extends Effect> Parser<Function<Selector, Effect>> subjectVerb(
            Parser<A> verbAndArg, BiFunction<Selector, A, E> ctor) {
        return verbAndArg.map(arg -> subject -> ctor.apply(subject, arg));
    }

    /// No-arg variant of [#subjectVerb] for verbs that carry no per-
    /// clause data — the verb phrase is consumed for its side effect
    /// and the constructor is applied to the bound subject alone.
    /// "Take an extra turn after this one." uses this shape. Named
    /// distinctly from [#subjectVerb] to avoid overload-resolution
    /// ambiguity (the 2-arg version is generic on a `Parser<A>` arg
    /// and a `BiFunction` ctor; with both overloads in scope, a
    /// no-arg call site with a method reference resolved ambiguously
    /// against `?` widening).
    public static <E extends Effect> Parser<Function<Selector, Effect>> subjectVerbNoArg(
            Parser<?> verbAlone, Function<Selector, E> ctor) {
        Function<Selector, Effect> wrapped = ctor::apply;
        return verbAlone.thenReturn(wrapped);
    }

    private static Effect distribute(Selector subject, List<Function<Selector, Effect>> verbs) {
        if (verbs.size() == 1) {
            return verbs.getFirst().apply(subject);
        }
        Selector placeholder = pickPlaceholder(subject);
        return new SharedSubjectEffect(
                subject, verbs.stream().map(v -> v.apply(placeholder)).toList());
    }

    /// Pick the per-axis "shared participant" sentinel matching
    /// `subject`'s axis — [PlayerSelector.Bound#PLAYER] for
    /// player-axis selectors, [ObjectSelector.Bound#OBJECT]
    /// for object-axis. Used by [#distribute] (subject distribution
    /// in [SharedSubjectEffect]) and by [#VERB_CHOICE] (shared-target
    /// distribution in [be.imgn.mtg.engine.oracle2.domain.effect.ChoiceEffect]).
    /// Recurses through [QuantifierSelector] / `Target` wrappers so
    /// "each opponent draws and loses life" picks the player-axis
    /// sentinel from the inner `OPPONENT`. [Selector.AllOf] /
    /// [Selector.OneOf] as shared participants are real in
    /// principle but absent from current oracle text — defensive
    /// throw until a real card forces the design.
    private static Selector pickPlaceholder(Selector subject) {
        return switch (subject) {
            case PlayerSelector p -> PlayerSelector.Bound.PLAYER;
            case ObjectSelector o -> ObjectSelector.Bound.OBJECT;
            case QuantifierSelector q -> pickPlaceholder(q.selector());
            case Selector.AllOf a -> throw new IllegalStateException("AllOf as shared subject not yet supported: " + a);
            case Selector.OneOf a -> throw new IllegalStateException("OneOf as shared subject not yet supported: " + a);
            case Selector.OneOrMoreOf a ->
                throw new IllegalStateException("OneOrMoreOf as shared subject not yet supported: " + a);
        };
    }
}
