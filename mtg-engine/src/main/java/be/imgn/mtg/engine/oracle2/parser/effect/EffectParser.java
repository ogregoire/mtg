package be.imgn.mtg.engine.oracle2.parser.effect;

import static be.imgn.mtg.engine.oracle2.parser.Parsers.andList;
import static be.imgn.mtg.engine.oracle2.parser.Parsers.phrase;
import static com.google.common.labs.parse.Parser.anyOf;
import static com.google.common.labs.parse.Parser.sequence;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import com.google.common.labs.parse.Parser;

import be.imgn.mtg.engine.oracle2.domain.effect.Effect;
import be.imgn.mtg.engine.oracle2.domain.effect.SharedSubjectEffect;
import be.imgn.mtg.engine.oracle2.domain.selector.ObjectSelector;
import be.imgn.mtg.engine.oracle2.domain.selector.PlayerSelector;
import be.imgn.mtg.engine.oracle2.domain.selector.QuantifierSelector;
import be.imgn.mtg.engine.oracle2.domain.selector.Selector;
import be.imgn.mtg.engine.oracle2.parser.selector.SelectorParser;

/// Top-level entry for the oracle2 effect grammar.
///
/// Two surface shapes:
///
/// 1. **Verb-first**: "Destroy SELECTOR.", "Exile SELECTOR.", "Draw N
///    cards." — the verb is the first word and the subject (if any)
///    is implicit ("you" for Draw).
/// 2. **Subject-led**: "SUBJECT verb …" — a leading [Selector] subject
///    followed by one or more verb clauses joined by "and". A single
///    verb collapses to the bare effect with the subject inlined; two
///    or more verbs fan out into a [SharedSubjectEffect] with the
///    subject captured exactly once and each clause referencing it
///    via the matching per-axis sentinel
///    ([PlayerSelector.SharedSubject] / [ObjectSelector.SharedSubject]).
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
            SacrificeEffectParser.SACRIFICES_FN,
            GainLifeEffectParser.GAINS_LIFE_FN,
            LoseLifeEffectParser.LOSES_LIFE_FN);

    /// Verb-first imperatives — no subject parsed.
    private static final Parser<Effect> VERB_FIRST = anyOf(
            DestroyEffectParser.DESTROY.map(e -> (Effect) e),
            ExileEffectParser.EXILE.map(e -> (Effect) e),
            DrawEffectParser.DRAW_IMPERATIVE.map(e -> (Effect) e));

    /// "SUBJECT verb1 X [, verb2 Y[, and verb3 Z]]." — distributes the
    /// subject across each verb clause. Single verb returns the bare
    /// effect; multi-verb wraps in [SharedSubjectEffect].
    private static final Parser<Effect> SUBJECT_LED =
            sequence(SelectorParser.SELECTOR, andList(SUBJECT_VERB), EffectParser::distribute);

    /// Single sentence, terminating period required.
    public static final Parser<Effect> EFFECT = anyOf(VERB_FIRST, SUBJECT_LED).followedBy(phrase("."));

    /// Canonical shape for a subject-led verb clause. Use this for
    /// every new `*_FN` constant — the resulting parser slots into
    /// [#SUBJECT_VERB] without further wiring, and the binary
    /// constructor signature makes the (subject, arg) ordering
    /// impossible to swap by accident.
    public static <A, E extends Effect> Parser<Function<Selector, Effect>> subjectVerb(
            Parser<A> verbAndArg, BiFunction<Selector, A, E> ctor) {
        return verbAndArg.map(arg -> subject -> ctor.apply(subject, arg));
    }

    private static Effect distribute(Selector subject, List<Function<Selector, Effect>> verbs) {
        if (verbs.size() == 1) {
            return verbs.getFirst().apply(subject);
        }
        Selector placeholder = pickPlaceholder(subject);
        return new SharedSubjectEffect(
                subject, verbs.stream().map(v -> v.apply(placeholder)).toList());
    }

    /// Pick the per-axis [SharedSubject][PlayerSelector.SharedSubject]
    /// sentinel matching `subject`'s axis. Recurses through
    /// [QuantifierSelector] wrappers so "each opponent draws and
    /// loses life" picks the player-axis sentinel from the inner
    /// `OPPONENT`. [Selector.AllOf] / [Selector.AnyOf] as shared
    /// subjects are real in principle but absent from current
    /// oracle text — defensive throw until a real card forces the
    /// design.
    private static Selector pickPlaceholder(Selector subject) {
        return switch (subject) {
            case PlayerSelector p -> PlayerSelector.SharedSubject.INSTANCE;
            case ObjectSelector o -> ObjectSelector.SharedSubject.INSTANCE;
            case QuantifierSelector q -> pickPlaceholder(q.selector());
            case Selector.AllOf a -> throw new IllegalStateException("AllOf as shared subject not yet supported: " + a);
            case Selector.AnyOf a -> throw new IllegalStateException("AnyOf as shared subject not yet supported: " + a);
        };
    }
}
