package be.imgn.mtg.engine.oracle.parser;

import static be.imgn.mtg.engine.oracle.parser.SelectorParsers.AMOUNT;
import static be.imgn.mtg.engine.oracle.parser.SelectorParsers.COUNTER_TYPE;
import static be.imgn.mtg.engine.oracle.parser.Words.phrase;
import static com.google.common.labs.parse.Parser.anyOf;
import static com.google.common.labs.parse.Parser.sequence;
import static com.google.common.labs.parse.Parser.string;
import static com.google.common.labs.parse.Parser.word;

import java.util.List;

import com.google.common.labs.parse.Parser;

import be.imgn.mtg.engine.oracle.domain.Amount;
import be.imgn.mtg.engine.oracle.domain.CounterType;
import be.imgn.mtg.engine.oracle.domain.Effect;

/// Leaf-effect parsers for counters: add, distribute, and remove. Emits
/// [Effect.AddCounters], [Effect.DistributeCounters], and
/// [Effect.RemoveCounters] values consumed by [EffectParsers#BASE_EFFECT]
/// / [EffectParsers#CLAUSE].
final class CounterEffectParsers {
    private CounterEffectParsers() {}

    // ── Add counters ──────────────────────────────────────────────────

    /// Body of a single "put counter on target" clause — the "N T
    /// counter on S" run that appears after "Put" and after "and" in
    /// the separate-pair form. Shared with [#ADD_COUNTERS_PUT] so the
    /// two stay in sync.
    private static final Parser<Effect.AddCounters> PUT_COUNTER_BODY = sequence(
            AMOUNT, COUNTER_TYPE.followedBy(phrase("counter(s) on")), SubjectParsers.SUBJECT, Effect.AddCounters::new);

    /// "Put [N] [type] counter(s) on [target]." — the standard active-voice
    /// form used for most counter placements.
    private static final Parser<Effect.AddCounters> ADD_COUNTERS_PUT =
            phrase("Put").then(PUT_COUNTER_BODY);

    /// "[subject] gets [N] [type] counter(s) [, rounded up/down]?." —
    /// passive-voice form (Prologue to Phyresis; Contaminated Drink). The
    /// optional ", rounded up/down" applies to an enclosing
    /// [Amount.Half] (default UP); non-Half amounts simply consume it
    /// as flavor.
    private static final Parser<Effect.AddCounters> ADD_COUNTERS_GETS = sequence(
                    SubjectParsers.SUBJECT.followedBy(phrase("get(s)")),
                    AMOUNT,
                    COUNTER_TYPE.followedBy(phrase("counter(s)")),
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

    /// "Put [N₁] [t₁] counter on [S₁] and [N₂] [t₂] counter on [S₂]." —
    /// two counter placements on distinct targets (Serrated Biskelion:
    /// "Put a -1/-1 counter on this creature and a -1/-1 counter on
    /// target creature."). Each clause shares the single leading "Put"
    /// via [#PUT_COUNTER_BODY]. Emits the two [Effect.AddCounters]
    /// flattened into the enclosing effect list.
    static final Parser<List<Effect>> ADD_COUNTERS_SEPARATE_PAIR = sequence(
            phrase("Put").then(PUT_COUNTER_BODY).followedBy(word("and")),
            PUT_COUNTER_BODY,
            (first, second) -> List.of(first, second));

    /// "Put \[N₁\] \[t₁\] counter or \[N₂\] \[t₂\] counter on \[target\]." —
    /// disjunctive choice between two counter placements on a shared
    /// target (Dwarven Armorer). Emits a single [Effect.AddCounterChoice]
    /// with the two [Effect.AddCounters] options.
    static final Parser<Effect.AddCounterChoice> ADD_COUNTERS_CHOICE = sequence(
            phrase("Put").then(AMOUNT),
            COUNTER_TYPE.followedBy(phrase("counter(s) or")),
            sequence(
                    AMOUNT,
                    COUNTER_TYPE.followedBy(phrase("counter(s) on")),
                    SubjectParsers.SUBJECT,
                    (amt2, t2, target) -> new Effect.AddCounters(amt2, t2, target)),
            (amt1, t1, second) ->
                    new Effect.AddCounterChoice(List.of(new Effect.AddCounters(amt1, t1, second.target()), second)));

    /// "Put [N₁] [t₁] counter and [N₂] [t₂] counter on [target]." — two
    /// counter kinds placed on a shared target (Unexpected Fangs: "Put a
    /// +1/+1 counter and a lifelink counter on target creature."). Emits
    /// two [Effect.AddCounters] sharing the target, flattened into
    /// the enclosing effect list.
    static final Parser<List<Effect>> ADD_COUNTERS_PAIR = sequence(
            phrase("Put").then(AMOUNT),
            COUNTER_TYPE.followedBy(phrase("counter(s) and")),
            sequence(
                    AMOUNT,
                    COUNTER_TYPE.followedBy(phrase("counter(s) on")),
                    SubjectParsers.SUBJECT,
                    (amt2, t2, target) -> new Effect.AddCounters(amt2, t2, target)),
            (amt1, t1, second) -> List.of(new Effect.AddCounters(amt1, t1, second.target()), second));

    /// One "(amount, counter type)" pair used by [#ADD_COUNTERS_LIST].
    private record CounterPair(Amount amount, CounterType type) {}

    private static final Parser<CounterPair> COUNTER_PAIR =
            sequence(AMOUNT, COUNTER_TYPE.followedBy(phrase("counter(s)")), CounterPair::new);

    /// "Put [pair, pair, …, and pair] on [target]." — Oxford-comma list
    /// of counter pairs sharing a single target (Gift of the Viper:
    /// "Put a +1/+1 counter, a reach counter, and a deathtouch counter
    /// on target creature."). Generalizes [#ADD_COUNTERS_PAIR] to
    /// arbitrary list length. Must precede [#ADD_COUNTERS_PAIR] in the
    /// CLAUSE dispatcher since both share the "Put" prefix.
    static final Parser<List<Effect>> ADD_COUNTERS_LIST = sequence(
                    phrase("Put").then(MtgParsers.andList(COUNTER_PAIR)),
                    phrase("on").then(SubjectParsers.SUBJECT),
                    (pairs, target) -> pairs.stream()
                            .<Effect>map(p -> new Effect.AddCounters(p.amount(), p.type(), target))
                            .toList())
            .suchThat(list -> list.size() >= 3, "three or more counter pairs");

    // ── Distribute ────────────────────────────────────────────────────

    /// "Distribute [N] [type] counters among [subject]." — Elven Rite,
    /// Cytoshape. The "among" subject typically uses a range/up-to
    /// quantifier to bound the target count.
    static final Parser<Effect.DistributeCounters> DISTRIBUTE_COUNTERS = sequence(
            phrase("Distribute").then(AMOUNT),
            COUNTER_TYPE.followedBy(phrase("counter(s) among")),
            SubjectParsers.SUBJECT,
            Effect.DistributeCounters::new);

    // ── Remove counters ───────────────────────────────────────────────

    /// "Remove all counters from [subject]." — sweeping counter removal
    /// (Aether Snap). Modelled as a RemoveCounters with `all`
    /// reference amount and a placeholder counter type; callers should
    /// treat this as "every counter regardless of type".
    static final Parser<Effect.RemoveCounters> REMOVE_ALL_COUNTERS = phrase("Remove all counters from")
            .then(SubjectParsers.SUBJECT)
            .map(subj -> new Effect.RemoveCounters(Amount.All.ALL, CounterType.Any.ANY, subj));

    /// "\[subject\] lose(s) all \[type\] counters." — subject-side removal
    /// of all counters of a specific kind (Leeches: "Target player loses
    /// all poison counters."). Equivalent to Remove-all-from, but the
    /// subject comes first and the counter type is typed.
    static final Parser<Effect.RemoveCounters> LOSES_ALL_COUNTERS = sequence(
            SubjectParsers.SUBJECT.followedBy(phrase("lose(s) all")),
            COUNTER_TYPE.followedBy(phrase("counter(s)")),
            (subj, type) -> new Effect.RemoveCounters(Amount.All.ALL, type, subj));

    static final Parser<Effect.RemoveCounters> REMOVE_COUNTERS = sequence(
            phrase("Remove").then(AMOUNT),
            // Typed counter form: "remove N <type> counter(s) from X".
            // Untyped form (Render Inert: "Remove up to five counters from
            // target permanent.") falls back to a generic "any" counter.
            anyOf(
                    COUNTER_TYPE.followedBy(phrase("counter(s) from")),
                    phrase("counter(s) from").thenReturn(CounterType.Any.ANY)),
            SubjectParsers.SUBJECT,
            Effect.RemoveCounters::new);
}
