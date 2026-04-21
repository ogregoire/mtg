package be.imgn.mtg.engine.oracle;

import static be.imgn.mtg.engine.oracle.Words.phrase;
import static com.google.common.labs.parse.Parser.anyOf;
import static com.google.common.labs.parse.Parser.sequence;
import static com.google.common.labs.parse.Parser.string;

import java.util.List;

import com.google.common.labs.parse.Parser;

/// Leaf-effect parsers for counters: add, distribute, and remove. Emits
/// [Effect.AddCounters], [Effect.DistributeCounters], and
/// [Effect.RemoveCounters] values consumed by [EffectParsers#BASE_EFFECT]
/// / [EffectParsers#CLAUSE].
final class CounterEffectParsers {
    private CounterEffectParsers() {}

    // ── Add counters ──────────────────────────────────────────────────

    /// "Put [N] [type] counter(s) on [target]." — the standard active-voice
    /// form used for most counter placements.
    private static final Parser<Effect.AddCounters> ADD_COUNTERS_PUT = sequence(
            phrase("Put").then(SelectorParsers.AMOUNT),
            SelectorParsers.COUNTER_TYPE.followedBy(phrase("counter(s) on")),
            SubjectParsers.SUBJECT,
            Effect.AddCounters::new);

    /// "[subject] gets [N] [type] counter(s) [, rounded up/down]?." —
    /// passive-voice form (Prologue to Phyresis; Contaminated Drink). The
    /// optional ", rounded up/down" applies to an enclosing
    /// [Amount.Half] (default UP); non-Half amounts simply consume it
    /// as flavor.
    private static final Parser<Effect.AddCounters> ADD_COUNTERS_GETS = sequence(
                    SubjectParsers.SUBJECT.followedBy(phrase("get(s)")),
                    SelectorParsers.AMOUNT,
                    SelectorParsers.COUNTER_TYPE.followedBy(phrase("counter(s)")),
                    (target, amount, type) -> new Effect.AddCounters(amount, type, target))
            .optionallyFollowedBy(string(",").then(phrase("rounded [up|down]")), (ac, _) -> ac);

    static final Parser<Effect.AddCounters> ADD_COUNTERS = anyOf(ADD_COUNTERS_PUT, ADD_COUNTERS_GETS)
            // Optional trailing "for each X" multiplier (Immaculate
            // Magistrate: "Put a +1/+1 counter on target creature for
            // each Elf you control."). Replaces the base count with a
            // count-of expression.
            .optionallyFollowedBy(
                    CountOfParsers.FOR_EACH, (ac, each) -> new Effect.AddCounters(each, ac.type(), ac.target()))
            // Optional trailing ", where X is <def>" — binds X in a
            // variable count (Soul's Might: "Put X +1/+1 counters on
            // target creature, where X is that creature's power.").
            .optionallyFollowedBy(CountOfParsers.WHERE_X_IS, Effect.AddCounters::withXDefinition);

    /// "Put [N₁] [t₁] counter and [N₂] [t₂] counter on [target]." — two
    /// counter kinds placed on a shared target (Unexpected Fangs: "Put a
    /// +1/+1 counter and a lifelink counter on target creature."). Emits
    /// two [Effect.AddCounters] sharing the target, flattened into
    /// the enclosing effect list.
    static final Parser<List<Effect>> ADD_COUNTERS_PAIR = sequence(
            phrase("Put").then(SelectorParsers.AMOUNT),
            SelectorParsers.COUNTER_TYPE.followedBy(phrase("counter(s) and")),
            sequence(
                    SelectorParsers.AMOUNT,
                    SelectorParsers.COUNTER_TYPE.followedBy(phrase("counter(s) on")),
                    SubjectParsers.SUBJECT,
                    (amt2, t2, target) -> new Effect.AddCounters(amt2, t2, target)),
            (amt1, t1, second) -> List.of(new Effect.AddCounters(amt1, t1, second.target()), second));

    // ── Distribute ────────────────────────────────────────────────────

    /// "Distribute [N] [type] counters among [subject]." — Elven Rite,
    /// Cytoshape. The "among" subject typically uses a range/up-to
    /// quantifier to bound the target count.
    static final Parser<Effect.DistributeCounters> DISTRIBUTE_COUNTERS = sequence(
            phrase("Distribute").then(SelectorParsers.AMOUNT),
            SelectorParsers.COUNTER_TYPE.followedBy(phrase("counter(s) among")),
            SubjectParsers.SUBJECT,
            Effect.DistributeCounters::new);

    // ── Remove counters ───────────────────────────────────────────────

    /// "Remove all counters from [subject]." — sweeping counter removal
    /// (Aether Snap). Modelled as a RemoveCounters with `all`
    /// reference amount and a placeholder counter type; callers should
    /// treat this as "every counter regardless of type".
    static final Parser<Effect.RemoveCounters> REMOVE_ALL_COUNTERS = phrase("Remove all counters from")
            .then(SubjectParsers.SUBJECT)
            .map(subj -> new Effect.RemoveCounters(Amount.reference("all"), CounterType.named("any"), subj));

    static final Parser<Effect.RemoveCounters> REMOVE_COUNTERS = sequence(
            phrase("Remove").then(SelectorParsers.AMOUNT),
            // Typed counter form: "remove N <type> counter(s) from X".
            // Untyped form (Render Inert: "Remove up to five counters from
            // target permanent.") falls back to a generic "any" counter.
            anyOf(
                    SelectorParsers.COUNTER_TYPE.followedBy(phrase("counter(s) from")),
                    phrase("counter(s) from").thenReturn(CounterType.named("any"))),
            SubjectParsers.SUBJECT,
            Effect.RemoveCounters::new);
}
