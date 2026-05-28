package be.imgn.mtg.engine.oracle2.parser.effect;

import static be.imgn.mtg.engine.oracle2.parser.AmountParser.AMOUNT;
import static be.imgn.mtg.engine.oracle2.parser.AmountParser.FOR_EACH;
import static be.imgn.mtg.engine.oracle2.parser.Parsers.phrase;

import java.util.function.Function;

import com.google.common.labs.parse.Parser;

import be.imgn.mtg.engine.oracle2.domain.effect.DrawEffect;
import be.imgn.mtg.engine.oracle2.domain.effect.Effect;
import be.imgn.mtg.engine.oracle2.domain.selector.Selector;

/// Parser for [DrawEffect] ({@mtg.rule 121}). Subject-led only:
/// "X draws N cards." or bare "Draw N cards." (where [EffectParser]
/// supplies the implicit "you" subject).
///
/// Supports the optional `for each (subject)` tail (Mass Appeal:
/// "Draw a card for each Human you control."). When present, the
/// total amount becomes [be.imgn.mtg.engine.oracle2.domain.Amount.CountOf]
/// of the for-each subject — the leading numeric ("a" / "1") is
/// discarded because the count *is* the amount. Forms with a
/// multiplicative leading numeric ("Draw two cards for each X")
/// would need [be.imgn.mtg.engine.oracle2.domain.Amount.Plus] /
/// `Times` and are out of scope.
public final class DrawEffectParser {
    private DrawEffectParser() {}

    /// "draw(s) N card(s) [for each (subject)]?" — subject-led
    /// wrapper. Handles both imperative "Draw" (no explicit subject;
    /// [EffectParser] supplies implicit YOU) and inflected "draws"
    /// (subject precedes).
    public static final Parser<Function<Selector, Effect>> DRAWS_FN = EffectParser.subjectVerb(
            phrase("draw(s)")
                    .then(AMOUNT)
                    .followedBy(phrase("card(s)"))
                    .optionallyFollowedBy(FOR_EACH, (base, forEach) -> forEach),
            DrawEffect::new);
}
