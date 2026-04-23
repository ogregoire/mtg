package be.imgn.mtg.engine.oracle.parser;

import static be.imgn.mtg.engine.oracle.parser.Words.*;
import static com.google.common.labs.parse.Parser.anyOf;
import static com.google.common.labs.parse.Parser.digits;
import static com.google.common.labs.parse.Parser.one;
import static com.google.common.labs.parse.Parser.sequence;
import static com.google.common.labs.parse.Parser.word;

import com.google.common.labs.parse.Parser;
import com.google.mu.util.CharPredicate;

import be.imgn.mtg.engine.oracle.domain.Amount;
import be.imgn.mtg.engine.oracle.domain.Discarded;
import be.imgn.mtg.engine.oracle.domain.Effect;

/// Parsers for [Amount] expressions and the two primitives they build
/// on: plain integers (`INTEGER`, `SIGNED_INT`) and word numbers
/// (`WORD_NUMBER`, `NUMBER`). Extracted from [SelectorParsers] so the
/// amount grammar can be reasoned about in isolation.
final class AmountParsers {
    private AmountParsers() {}

    // ── Integer primitives ─────────────────────────────────────────────

    public static final Parser<Integer> INTEGER = digits().map(Integer::parseInt);

    /// Signed integer — `+N`, `-N`, or `−N`. The minus alternative
    /// accepts both the ASCII hyphen (used by P/T markers like
    /// `+1/-1`) and the U+2212 typographic minus (used by Scryfall on
    /// planeswalker loyalty costs). The sign token itself is
    /// non-capturing — only the combined signed value is returned.
    private static final CharPredicate MINUS_SIGN = CharPredicate.anyOf("-\u2212");

    public static final Parser<Integer> SIGNED_INT = sequence(
            anyOf(one('+').thenReturn(1), one(MINUS_SIGN, "minus").thenReturn(-1)), INTEGER, (sign, num) -> sign * num);

    public static final Parser<Integer> WORD_NUMBER = anyOf(
            phrase("One").thenReturn(1),
            phrase("Two").thenReturn(2),
            phrase("Three").thenReturn(3),
            phrase("Four").thenReturn(4),
            phrase("Five").thenReturn(5),
            phrase("Six").thenReturn(6),
            phrase("Seven").thenReturn(7),
            phrase("Eight").thenReturn(8),
            phrase("Nine").thenReturn(9),
            phrase("Ten").thenReturn(10),
            phrase("Eleven").thenReturn(11),
            phrase("Twelve").thenReturn(12),
            phrase("Thirteen").thenReturn(13),
            phrase("Fourteen").thenReturn(14),
            phrase("Fifteen").thenReturn(15),
            phrase("Sixteen").thenReturn(16),
            phrase("Seventeen").thenReturn(17),
            phrase("Eighteen").thenReturn(18),
            phrase("Nineteen").thenReturn(19),
            phrase("Twenty").thenReturn(20));

    /// An integer written either as digits (`3`) or an English word
    /// number (`three`). Prefer this when oracle text accepts both.
    public static final Parser<Integer> NUMBER = anyOf(WORD_NUMBER, INTEGER);

    // ── Amount ─────────────────────────────────────────────────────────

    /// "half [amount]" — scalar halving. `rounding` is left unset
    /// (`null`); downstream either binds it via the inline
    /// ", rounded up/down" suffix ([CountOfParsers#ROUNDING_DIRECTION])
    /// or via the trailing "Round up/down each time." directive
    /// applied by [#roundAmount(Effect , Amount.Half.Rounding)].
    private static final Parser<Amount> HALF_ATOM = word("half")
            .then(anyOf(
                    word("X").thenReturn(Amount.variable()),
                    WORD_NUMBER.map(Amount::exact),
                    INTEGER.map(Amount::exact)))
            .<Amount>map(Amount.Half::new);

    /// "N or more" — lower-bound amount (Military Intelligence: "attack
    /// with two or more creatures"). Must precede bare number atoms so
    /// the "or more" tail isn't left for an outer "or".
    private static final Parser<Amount> AT_LEAST_ATOM =
            NUMBER.followedBy(phrase("or more")).map(Amount.AtLeast::new);

    /// "N or M" — range amount bounded on both sides (Storm of Steel:
    /// "each of one or two targets"). Tried alongside AT_LEAST_ATOM.
    private static final Parser<Amount> RANGE_ATOM = sequence(NUMBER, word("or").then(NUMBER), Amount.Range::new);

    /// "up to N" — upper-bound amount (Render Inert: "Remove up to five
    /// counters from target permanent.").
    private static final Parser<Amount> UP_TO_ATOM =
            phrase("up to").then(NUMBER).map(Amount.UpTo::new);

    /// "twice [base]" / "N times [base]" — multiplicative atom (Boon
    /// Reflection: "you gain twice that much life."; Crackle with
    /// Power: "deals five times X damage to each of up to X
    /// targets."). The leading multiplier is either the irregular
    /// `twice` or a word-number/integer followed by `times`.
    private static final Parser<Amount> TIMES_ATOM = sequence(
            anyOf(word("twice").thenReturn(2), NUMBER.followedBy(word("times"))),
            anyOf(
                    word("that")
                            .then(anyOf(
                                    phrase("much").thenReturn(Amount.reference("that much")),
                                    phrase("many").thenReturn(Amount.reference("that many")))),
                    word("X").thenReturn(Amount.variable()),
                    WORD_NUMBER.map(Amount::exact),
                    INTEGER.map(Amount::exact)),
            Amount.Times::new);

    /// A single-term amount — the atom before the optional `plus` suffix.
    private static final Parser<Amount> ATOMIC_AMOUNT = anyOf(
            HALF_ATOM, // must precede INTEGER/WORD_NUMBER — "half" is a word.
            UP_TO_ATOM,
            TIMES_ATOM,
            AT_LEAST_ATOM, // must precede RANGE_ATOM (more specific "or more" tail).
            RANGE_ATOM, // must precede bare WORD_NUMBER/INTEGER so "N or M" wins.
            word("X").thenReturn(Amount.variable()),
            word("that")
                    .then(anyOf(
                            phrase("much").thenReturn(Amount.reference("that much")),
                            phrase("many").thenReturn(Amount.reference("that many")))),
            WORD_NUMBER.map(Amount::exact),
            INTEGER.map(Amount::exact),
            phrase("[a|an]").thenReturn(Amount.exact(1)));

    /// Amount expression, optionally followed by `plus <atom>` for
    /// arithmetic like "X plus 3".
    public static final Parser<Amount> AMOUNT =
            ATOMIC_AMOUNT.optionallyFollowedBy(word("plus").then(ATOMIC_AMOUNT), Amount.Plus::new);

    // ── roundAmount walker ────────────────────────────────────────────
    // Post-parse specialization of unspecialized Amount.Half nodes. A
    // trailing "Round up/down each time." sentence in oracle text
    // (Peer into the Abyss, Hydroid Krasis) applies uniformly to
    // every Half whose rounding is still null. Halves that already
    // carry an inline ", rounded up/down" value are left alone.

    /// Returns `e` with every unspecialized [Amount.Half] buried
    /// anywhere inside its Amount-valued components set to `rounding`.
    /// `Effect` variants that hold no [Amount] are returned unchanged.
    static Effect roundAmount(Effect e, Amount.Half.Rounding rounding) {
        return switch (e) {
            case Effect.Sacrifice(var who, var what, var at, var scaleBy)
            when scaleBy != null -> new Effect.Sacrifice(who, what, at, roundAmount(scaleBy, rounding));
            case Effect.DealDamage(var source, var amt, var target, var atRandom) ->
                new Effect.DealDamage(source, roundAmount(amt, rounding), target, atRandom);
            case Effect.DealDividedDamage(var source, var total, var targets) ->
                new Effect.DealDividedDamage(source, roundAmount(total, rounding), targets);
            case Effect.GainLife(var player, var amt) -> new Effect.GainLife(player, roundAmount(amt, rounding));
            case Effect.LoseLife(var player, var amt) -> new Effect.LoseLife(player, roundAmount(amt, rounding));
            case Effect.Draw(var player, var amt) -> new Effect.Draw(player, roundAmount(amt, rounding));
            case Effect.Discard(var player, var discarded) ->
                new Effect.Discard(player, roundDiscarded(discarded, rounding));
            case Effect.Mill(var player, var amt, var xDef) ->
                new Effect.Mill(player, roundAmount(amt, rounding), xDef == null ? null : roundAmount(xDef, rounding));
            case Effect.Scry(var amt) -> new Effect.Scry(roundAmount(amt, rounding));
            case Effect.Surveil(var amt) -> new Effect.Surveil(roundAmount(amt, rounding));
            case Effect.AddCounters(var count, var type, var target, var xDef) ->
                new Effect.AddCounters(
                        roundAmount(count, rounding), type, target, xDef == null ? null : roundAmount(xDef, rounding));
            case Effect.RemoveCounters(var count, var type, var target) ->
                new Effect.RemoveCounters(roundAmount(count, rounding), type, target);
            case Effect.DistributeCounters(var count, var type, var among) ->
                new Effect.DistributeCounters(roundAmount(count, rounding), type, among);
            case Effect.CreateToken(var count, var token, var tapped) ->
                new Effect.CreateToken(roundAmount(count, rounding), token, tapped);
            case Effect.FlipCoins(var count, var ignore) -> new Effect.FlipCoins(roundAmount(count, rounding), ignore);
            case Effect.MoveCounters(var count, var type, var from, var onto) ->
                new Effect.MoveCounters(roundAmount(count, rounding), type, from, onto);
            case Effect.EnterWithCounters(var subject, var count, var type) ->
                new Effect.EnterWithCounters(subject, roundAmount(count, rounding), type);
            case Effect.DefineX(var amt) -> new Effect.DefineX(roundAmount(amt, rounding));
            case Effect.TakeExtraTurn(var player, var count) ->
                new Effect.TakeExtraTurn(player, roundAmount(count, rounding));
            case Effect.PlayAdditionalLands(var player, var count, var duration) ->
                new Effect.PlayAdditionalLands(player, roundAmount(count, rounding), duration);
            case Effect.GetMarker(var player, var count, var marker) ->
                new Effect.GetMarker(player, roundAmount(count, rounding), marker);
            case Effect.AttackLimit(var max, var whom) -> new Effect.AttackLimit(roundAmount(max, rounding), whom);
            case Effect.SetPropertyValue(var subject, var property, var value) ->
                new Effect.SetPropertyValue(subject, property, roundAmount(value, rounding));
            case Effect.ActivationLimit(var max) -> new Effect.ActivationLimit(roundAmount(max, rounding));
            case Effect.AdditionalEtbTriggers(var clause, var additional) ->
                new Effect.AdditionalEtbTriggers(clause, roundAmount(additional, rounding));
            case Effect.AbilityKindTriggersAdditional(var kind, var scope, var additional) ->
                new Effect.AbilityKindTriggersAdditional(kind, scope, roundAmount(additional, rounding));
            case Effect.LifeTotalBecomes(var player, var value) ->
                new Effect.LifeTotalBecomes(player, roundAmount(value, rounding));
            default -> e;
        };
    }

    /// Returns `a` with every unspecialized [Amount.Half] set to
    /// `rounding`. Recurses into [Amount.Half#base()] and the other
    /// Amount-containing variants ([Amount.Plus], [Amount.Times]) so
    /// deeply-nested halves are specialized too.
    static Amount roundAmount(Amount a, Amount.Half.Rounding rounding) {
        return switch (a) {
            case Amount.Half(var base, var existing) ->
                new Amount.Half(roundAmount(base, rounding), existing == null ? rounding : existing);
            case Amount.Plus(var left, var right) ->
                new Amount.Plus(roundAmount(left, rounding), roundAmount(right, rounding));
            case Amount.Times(var factor, var base) -> new Amount.Times(factor, roundAmount(base, rounding));
            default -> a;
        };
    }

    /// Recurses through a [Discarded] wrapper so `Discarded.Cards`'
    /// count is specialized.
    private static Discarded roundDiscarded(Discarded d, Amount.Half.Rounding rounding) {
        return d instanceof Discarded.Cards(var amt, var atRandom)
                ? new Discarded.Cards(roundAmount(amt, rounding), atRandom)
                : d;
    }
}
