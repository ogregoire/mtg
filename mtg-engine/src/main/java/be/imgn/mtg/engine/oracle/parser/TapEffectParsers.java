package be.imgn.mtg.engine.oracle.parser;

import static be.imgn.mtg.engine.oracle.parser.Words.phrase;
import static com.google.common.labs.parse.Parser.anyOf;
import static com.google.common.labs.parse.Parser.sequence;
import static com.google.common.labs.parse.Parser.word;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.common.labs.parse.Parser;

import be.imgn.mtg.engine.oracle.domain.Effect;
import be.imgn.mtg.engine.oracle.domain.Subject;

/// Leaf-effect parsers for tap / untap state changes. Produces
/// [Effect.Tap] / [Effect.Untap] values consumed by
/// [EffectParsers#BASE_EFFECT] / [EffectParsers#MAY]. The oracle
/// verb ("Tap" / "Untap") is factored into [#TAP_VERB] / [#UNTAP_VERB]
/// so the "Tap or untap target" chooser-at-resolution shape composes
/// from the same pieces — one or more verbs joined by `or`, sharing
/// a single trailing subject — via [#CHANGE_TAP_STATES].
final class TapEffectParsers {
    private TapEffectParsers() {}

    /// Trailing "during \[scope\]" flavor suffix (Thousand Moons
    /// Infantry: "Untap Thousand Moons Infantry during each opponent's
    /// untap step."). Consumed as flavor.
    private static final Parser<String> DURING_SCOPE =
            word("during").then(EffectParsers.WORD_OR_CONTRACTION.atLeastOnce().map(words -> String.join(" ", words)));

    /// Leading verb → Effect constructor. Shared between [#TAP] and
    /// [#CHANGE_TAP_STATES] so the oracle phrase literal ("Tap") is
    /// written once and the `or`-composition in [#CHANGE_TAP_STATES]
    /// reuses the same builder.
    private static final Parser<Function<Subject, Effect>> TAP_VERB =
            phrase("Tap").<Function<Subject, Effect>>thenReturn(Effect.Tap::new);

    /// Leading verb → Effect constructor, symmetric counterpart to [#TAP_VERB].
    private static final Parser<Function<Subject, Effect>> UNTAP_VERB =
            phrase("Untap").<Function<Subject, Effect>>thenReturn(Effect.Untap::new);

    /// "Tap \[target\]." — e.g., Twiddle.
    static final Parser<Effect.Tap> TAP = sequence(
                    TAP_VERB, SubjectParsers.SUBJECT, (fn, s) -> (Effect.Tap) fn.apply(s))
            .optionallyFollowedBy(DURING_SCOPE, (t, _) -> t);

    /// "Untap \[target\]." and the player-initiated "\[player\] untaps
    /// \[target\]" (Early Harvest). Also consumes the optional
    /// "during \[scope\]" flavor suffix.
    static final Parser<Effect.Untap> UNTAP = anyOf(
                    sequence(UNTAP_VERB, SubjectParsers.SUBJECT, (fn, s) -> (Effect.Untap) fn.apply(s)),
                    sequence(
                            SubjectParsers.PLAYER_SUBJECT.followedBy(phrase("untap(s)")),
                            SubjectParsers.SUBJECT,
                            (_, target) -> new Effect.Untap(target)))
            .optionallyFollowedBy(DURING_SCOPE, (u, _) -> u);

    /// Two or more tap-state verbs joined by `or`, sharing one trailing
    /// subject — the chooser-at-resolution "Tap or untap \[target\]"
    /// shape (Thassa's Ire, Puppeteer). The `.suchThat(size ≥ 2)` guard
    /// keeps single-verb matches from stealing input that the bare
    /// [#TAP] / [#UNTAP] parsers will handle instead.
    private static final Parser<List<Effect>> VERB_OR_VERB_SUBJECT = sequence(
            anyOf(TAP_VERB, UNTAP_VERB)
                    .atLeastOnceDelimitedBy("or", Collectors.toUnmodifiableList())
                    .suchThat(fns -> fns.size() >= 2, "two or more tap-state verbs joined by 'or'"),
            SubjectParsers.SUBJECT,
            (fns, subject) -> fns.stream().<Effect>map(fn -> fn.apply(subject)).toList());

    /// "\[You may\]? Tap or untap \[target\]." — fans the shared-subject
    /// verb chain into a list of peer effects (one per verb). The
    /// "You may" prefix is absorbed here (rather than going through
    /// [EffectParsers#MAY]) because `Effect.Optional` can only wrap a
    /// single Effect and this parser emits a pair. Must precede [#TAP]
    /// in the outer `anyOf` so "Tap or untap" wins over a bare [#TAP]
    /// that would otherwise leave "or untap" unconsumed.
    static final Parser<List<Effect>> CHANGE_TAP_STATES =
            anyOf(phrase("You may").then(VERB_OR_VERB_SUBJECT), VERB_OR_VERB_SUBJECT);
}
