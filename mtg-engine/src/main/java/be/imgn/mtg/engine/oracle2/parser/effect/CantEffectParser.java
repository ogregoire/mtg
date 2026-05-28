package be.imgn.mtg.engine.oracle2.parser.effect;

import static be.imgn.mtg.engine.oracle2.parser.Parsers.phrase;
import static com.google.common.labs.parse.Parser.anyOf;
import static com.google.common.labs.parse.Parser.sequence;

import java.util.function.Function;

import com.google.common.labs.parse.Parser;

import org.jspecify.annotations.Nullable;

import be.imgn.mtg.engine.oracle2.domain.Duration;
import be.imgn.mtg.engine.oracle2.domain.effect.CantAttackEffect;
import be.imgn.mtg.engine.oracle2.domain.effect.CantBeBlockedEffect;
import be.imgn.mtg.engine.oracle2.domain.effect.CantBeCounteredEffect;
import be.imgn.mtg.engine.oracle2.domain.effect.CantBlockEffect;
import be.imgn.mtg.engine.oracle2.domain.effect.CantCastEffect;
import be.imgn.mtg.engine.oracle2.domain.effect.CantCycleEffect;
import be.imgn.mtg.engine.oracle2.domain.effect.CantSearchLibraryEffect;
import be.imgn.mtg.engine.oracle2.domain.effect.Effect;
import be.imgn.mtg.engine.oracle2.domain.effect.MustBeBlockedEffect;
import be.imgn.mtg.engine.oracle2.domain.effect.SharedSubjectEffect;
import be.imgn.mtg.engine.oracle2.domain.selector.ObjectSelector;
import be.imgn.mtg.engine.oracle2.domain.selector.PlayerSelector;
import be.imgn.mtg.engine.oracle2.domain.selector.QuantifierSelector;
import be.imgn.mtg.engine.oracle2.domain.selector.Selector;
import be.imgn.mtg.engine.oracle2.parser.DurationParser;
import be.imgn.mtg.engine.oracle2.parser.selector.PlayerSelectorParser;
import be.imgn.mtg.engine.oracle2.parser.selector.SelectorParser;

/// Parsers for the per-verb continuous restriction effects of the
/// shape "(subject) can't (verb) (duration)?." ({@mtg.rule 614}).
/// Each verb has its own [Effect] arm — [CantBlockEffect],
/// [CantBeBlockedEffect], [CantCycleEffect],
/// [CantSearchLibraryEffect], [CantCastEffect] — so the engine can
/// case-match on the specific restriction without a shared
/// `Action` discriminator.
///
/// All five subject-led wrappers are registered individually in
/// [EffectParser#SUBJECT_VERB]; the SUBJECT_LED outer rule supplies
/// the subject and the trailing period. The trailing
/// `duration?` tail is parsed via `DURATION.orElse(PERMANENT)` so
/// every emitted record carries a non-null [Duration], defaulting
/// to [Duration.Fixed#PERMANENT].
public final class CantEffectParser {
    private CantEffectParser() {}

    /// Trailing duration tail — defaults to PERMANENT when absent.
    /// Shared so every Cant* arm uses the same default.
    private static final Parser<Duration>.OrEmpty DURATION_TAIL =
            DurationParser.DURATION.orElse(Duration.Fixed.PERMANENT);

    /// "can't block (what)? (duration)?" body — captures the
    /// optional what-selector ("can't block Humans") and trailing
    /// duration ("this turn"). Carrier record keeps the two-axis
    /// state local to this parser.
    private static final Parser<CantBlockBody> CANT_BLOCK_BODY = phrase("can't block")
            .thenReturn(new CantBlockBody(null, Duration.Fixed.PERMANENT))
            .optionallyFollowedBy(SelectorParser.SELECTOR, CantBlockBody::withWhat)
            .optionallyFollowedBy(DurationParser.DURATION, CantBlockBody::withDuration);

    /// "can't block (what)? (duration)?" — [CantBlockEffect.Of].
    public static final Parser<Function<Selector, Effect>> CANT_BLOCK_FN =
            EffectParser.subjectVerb(CANT_BLOCK_BODY, (subject, body) -> body.apply(subject));

    /// Internal carrier — withers keep the two-axis state local; the
    /// `apply(subject)` method threads in the bound subject to
    /// produce the final [CantBlockEffect.Of].
    private record CantBlockBody(@Nullable Selector what, Duration duration) {
        CantBlockBody withWhat(Selector what) {
            return new CantBlockBody(what, duration);
        }

        CantBlockBody withDuration(Duration duration) {
            return new CantBlockBody(what, duration);
        }

        CantBlockEffect.Of apply(Selector subject) {
            return new CantBlockEffect.Of(subject, what, duration);
        }
    }

    /// "can't attack (defender)? (duration)?" body — mirrors
    /// [CantBlockBody] for the attack side.
    private static final Parser<CantAttackBody> CANT_ATTACK_BODY = phrase("can't attack")
            .thenReturn(new CantAttackBody(null, Duration.Fixed.PERMANENT))
            .optionallyFollowedBy(PlayerSelectorParser.PLAYER_SELECTOR, CantAttackBody::withDefender)
            .optionallyFollowedBy(DurationParser.DURATION, CantAttackBody::withDuration);

    /// "can't attack (defender)? (duration)?" — [CantAttackEffect.Of].
    public static final Parser<Function<Selector, Effect>> CANT_ATTACK_FN =
            EffectParser.subjectVerb(CANT_ATTACK_BODY, (subject, body) -> body.apply(subject));

    private record CantAttackBody(@Nullable PlayerSelector defender, Duration duration) {
        CantAttackBody withDefender(PlayerSelector defender) {
            return new CantAttackBody(defender, duration);
        }

        CantAttackBody withDuration(Duration duration) {
            return new CantAttackBody(defender, duration);
        }

        CantAttackEffect.Of apply(Selector subject) {
            return new CantAttackEffect.Of(subject, defender, duration);
        }
    }

    /// "can't block alone (duration)?" — [CantBlockEffect.Alone].
    /// Registered before [#CANT_BLOCK_FN] in [EffectParser] so the
    /// longer "block alone" prefix wins over bare "block".
    public static final Parser<Function<Selector, Effect>> CANT_BLOCK_ALONE_FN =
            EffectParser.subjectVerb(phrase("can't block alone").then(DURATION_TAIL), CantBlockEffect.Alone::new);

    /// "can't attack alone (duration)?" — [CantAttackEffect.Alone].
    /// Registered before [#CANT_ATTACK_FN] in [EffectParser] so the
    /// longer "attack alone" prefix wins over bare "attack".
    public static final Parser<Function<Selector, Effect>> CANT_ATTACK_ALONE_FN =
            EffectParser.subjectVerb(phrase("can't attack alone").then(DURATION_TAIL), CantAttackEffect.Alone::new);

    /// "can't be blocked (duration)" — [CantBeBlockedEffect].
    /// Registered before [#CANT_BLOCK_FN] in [EffectParser] so the
    /// longer "be blocked" prefix wins over bare "block".
    public static final Parser<Function<Selector, Effect>> CANT_BE_BLOCKED_FN =
            EffectParser.subjectVerb(phrase("can't be blocked").then(DURATION_TAIL), CantBeBlockedEffect::new);

    /// "can't be countered (duration)" — [CantBeCounteredEffect].
    public static final Parser<Function<Selector, Effect>> CANT_BE_COUNTERED_FN =
            EffectParser.subjectVerb(phrase("can't be countered").then(DURATION_TAIL), CantBeCounteredEffect::new);

    /// "can't cycle cards (duration)" — [CantCycleEffect].
    public static final Parser<Function<Selector, Effect>> CANT_CYCLE_FN =
            EffectParser.subjectVerb(phrase("can't cycle cards").then(DURATION_TAIL), CantCycleEffect::new);

    /// "can't search libraries (duration)" — [CantSearchLibraryEffect].
    public static final Parser<Function<Selector, Effect>> CANT_SEARCH_LIBRARY_FN = EffectParser.subjectVerb(
            phrase("can't search libraries").then(DURATION_TAIL), CantSearchLibraryEffect::new);

    /// "must be blocked if able (duration)?" — [MustBeBlockedEffect].
    /// CR 509.1c. Obligation rather than restriction, but
    /// structurally identical to a Cant* arm.
    public static final Parser<Function<Selector, Effect>> MUST_BE_BLOCKED_FN =
            EffectParser.subjectVerb(phrase("must be blocked if able").then(DURATION_TAIL), MustBeBlockedEffect::new);

    // ── Compound "can't X or Y" ────────────────────────────────────

    /// One verb-body after the shared "can't" prefix. Each suffix
    /// emits a [Function] from the bound subject to the matching
    /// [Effect] arm. **Strict suffix** — no "can't" — so
    /// [#CANT_COMPOUND_FN] can re-use them after consuming the
    /// shared prefix once. Duplicates the body grammar of the
    /// per-verb CANT_*_FN parsers without "can't"; if more cant
    /// verbs need to participate in "or" composition, extract their
    /// body shapes the same way.
    private static final Parser<Function<Selector, Effect>> ATTACK_SUFFIX = phrase("attack")
            .thenReturn(new CantAttackBody(null, Duration.Fixed.PERMANENT))
            .optionallyFollowedBy(PlayerSelectorParser.PLAYER_SELECTOR, CantAttackBody::withDefender)
            .optionallyFollowedBy(DurationParser.DURATION, CantAttackBody::withDuration)
            .<Function<Selector, Effect>>map(body -> body::apply);

    private static final Parser<Function<Selector, Effect>> BLOCK_SUFFIX = phrase("block")
            .thenReturn(new CantBlockBody(null, Duration.Fixed.PERMANENT))
            .optionallyFollowedBy(SelectorParser.SELECTOR, CantBlockBody::withWhat)
            .optionallyFollowedBy(DurationParser.DURATION, CantBlockBody::withDuration)
            .<Function<Selector, Effect>>map(body -> body::apply);

    private static final Parser<Function<Selector, Effect>> SUFFIX = anyOf(ATTACK_SUFFIX, BLOCK_SUFFIX);

    /// "can't (X or Y [or Z]?)" — shared "can't" prefix over a list
    /// of verb-suffixes joined by "or" (CR Light of Day: "Black
    /// creatures can't attack or block." — oracle's "or" here means
    /// AND, so the AST emits one cant-effect per suffix wrapped in
    /// a [SharedSubjectEffect]). Size guard rejects the singleton
    /// case so it falls through to the per-verb arms.
    public static final Parser<Function<Selector, Effect>> CANT_COMPOUND_FN = phrase("can't")
            .then(SUFFIX.atLeastOnceDelimitedBy("or").suchThat(l -> l.size() >= 2, "cant-or-list (≥2)"))
            .<Function<Selector, Effect>>map(suffixes -> subject -> {
                Selector placeholder = boundPlaceholder(subject);
                return new SharedSubjectEffect(
                        subject,
                        suffixes.stream().map(fn -> fn.apply(placeholder)).toList());
            });

    /// Pick the per-axis Bound sentinel for `subject` — mirrors
    /// [EffectParser]'s private `pickPlaceholder`. Local copy avoids
    /// exposing internal helpers across packages.
    private static Selector boundPlaceholder(Selector subject) {
        return switch (subject) {
            case PlayerSelector ignored -> PlayerSelector.Bound.PLAYER;
            case ObjectSelector ignored -> ObjectSelector.Bound.OBJECT;
            case QuantifierSelector q -> boundPlaceholder(q.selector());
            default -> throw new IllegalStateException("cant-compound on unsupported subject: " + subject);
        };
    }

    /// "can't cast (selector) spells (duration)" — [CantCastEffect].
    /// Three-arg shape (subject, spells, duration) — the standard
    /// two-arg [EffectParser#subjectVerb] helper doesn't fit, so
    /// the verb body sequences the spells selector and duration
    /// into a carrier record, and the outer map threads in the
    /// subject.
    public static final Parser<Function<Selector, Effect>> CANT_CAST_FN = sequence(
                    phrase("can't cast").then(SelectorParser.SELECTOR), DURATION_TAIL, CastBody::new)
            .map(body -> subject -> new CantCastEffect(subject, body.spells(), body.duration()));

    /// Internal carrier — pairs the spells selector with the
    /// duration before the subject is threaded in.
    private record CastBody(Selector spells, Duration duration) {}
}
