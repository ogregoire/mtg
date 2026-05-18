package be.imgn.mtg.engine.oracle2.parser.effect;

import static be.imgn.mtg.engine.oracle2.parser.AmountParser.AMOUNT;
import static be.imgn.mtg.engine.oracle2.parser.Parsers.phrase;

import java.util.function.Function;

import com.google.common.labs.parse.Parser;

import be.imgn.mtg.engine.oracle2.domain.effect.Effect;
import be.imgn.mtg.engine.oracle2.domain.effect.ScryEffect;
import be.imgn.mtg.engine.oracle2.domain.selector.Selector;

/// Parser for [ScryEffect] ({@mtg.rule 701.22}). Subject-led only:
/// "X scries N." or bare "Scry N." (where [EffectParser] supplies the
/// implicit "you" subject). Uses bracket alternation because the
/// `y → ies` swap is not a suffix append (the `(s)` inflection form
/// would produce "scryies").
public final class ScryEffectParser {
    private ScryEffectParser() {}

    /// "scry/scries N" — subject-led wrapper. Handles imperative
    /// "Scry" (no explicit subject; [EffectParser] supplies implicit
    /// YOU) and inflected "scries" (subject precedes).
    public static final Parser<Function<Selector, Effect>> SCRIES_FN =
            EffectParser.subjectVerb(phrase("[scry|scries]").then(AMOUNT), ScryEffect::new);
}
