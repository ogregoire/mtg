package be.imgn.mtg.engine.oracle;

import static be.imgn.mtg.engine.oracle.Words.phrase;
import static com.google.common.labs.parse.Parser.anyOf;
import static com.google.common.labs.parse.Parser.sequence;
import static com.google.common.labs.parse.Parser.word;

import com.google.common.labs.parse.Parser;

/// Leaf-effect parsers for tap / untap / tap-or-untap state changes.
/// Produces [Effect.ChangeTapState] values consumed by
/// [EffectParsers#BASE_EFFECT] / [EffectParsers#MAY].
final class TapEffectParsers {
    private TapEffectParsers() {}

    /// "Tap \[target\]." — e.g., Twiddle.
    static final Parser<Effect.ChangeTapState> TAP = phrase("Tap")
            .then(SubjectParsers.SUBJECT)
            .map(s -> new Effect.ChangeTapState(s, Effect.ChangeTapState.Kind.TAP));

    /// "Untap \[target\]." and the player-initiated "\[player\] untaps
    /// \[target\]" (Early Harvest). Also consumes the optional
    /// "during \[scope\]" flavor suffix (Thousand Moons Infantry).
    static final Parser<Effect.ChangeTapState> UNTAP = anyOf(
                    phrase("Untap").then(SubjectParsers.SUBJECT),
                    sequence(
                            SubjectParsers.PLAYER_SUBJECT.followedBy(phrase("untap(s)")),
                            SubjectParsers.SUBJECT,
                            (_, target) -> target))
            .map(s -> new Effect.ChangeTapState(s, Effect.ChangeTapState.Kind.UNTAP))
            .optionallyFollowedBy(
                    word("during")
                            .then(EffectParsers.WORD_OR_CONTRACTION
                                    .atLeastOnce()
                                    .map(words -> String.join(" ", words))),
                    (u, _) -> u);

    /// "\[you may\]? tap or untap \[target\]." — chooser-at-resolution
    /// form (Thassa's Ire, Puppeteer). Must precede [#TAP] so
    /// "tap or untap" isn't consumed as a bare tap plus stray "or
    /// untap" tokens.
    static final Parser<Effect.ChangeTapState> TAP_OR_UNTAP = anyOf(
                    phrase("You may tap or untap"), phrase("Tap or untap"))
            .then(SubjectParsers.SUBJECT)
            .map(s -> new Effect.ChangeTapState(s, Effect.ChangeTapState.Kind.EITHER));
}
