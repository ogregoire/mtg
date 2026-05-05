package be.imgn.mtg.engine.oracle2.parser.effect;

import static be.imgn.mtg.engine.oracle2.parser.AmountParser.AMOUNT;
import static be.imgn.mtg.engine.oracle2.parser.Parsers.phrase;

import java.util.function.Function;

import com.google.common.labs.parse.Parser;

import be.imgn.mtg.engine.oracle2.domain.PlayerRelation;
import be.imgn.mtg.engine.oracle2.domain.effect.DrawEffect;
import be.imgn.mtg.engine.oracle2.domain.effect.Effect;
import be.imgn.mtg.engine.oracle2.domain.selector.PlayerRelationSelector;
import be.imgn.mtg.engine.oracle2.domain.selector.Selector;

/// Parser for [DrawEffect] ({@mtg.rule 121}). Two surface forms:
///
/// 1. **Verb-first** ("Draw three cards.") — implicit "you" subject;
///    [#DRAW_IMPERATIVE] fills `who` with [PlayerRelation#YOU].
/// 2. **Subject-led** ("Target player draws three cards.") —
///    [#DRAWS_FN] is a `Function<Selector, Effect>` that the
///    composer applies to whatever subject precedes it.
public final class DrawEffectParser {
    private DrawEffectParser() {}

    /// Implicit "you" subject for the bare imperative form.
    private static final Selector YOU = new PlayerRelationSelector(PlayerRelation.YOU);

    /// "Draw N card(s)" → [DrawEffect] with implicit YOU.
    public static final Parser<DrawEffect> DRAW_IMPERATIVE =
            phrase("Draw").then(AMOUNT).followedBy(phrase("card(s)")).map(amount -> new DrawEffect(YOU, amount));

    /// "draw(s) N card(s)" — subject-led wrapper.
    public static final Parser<Function<Selector, Effect>> DRAWS_FN =
            EffectParser.subjectVerb(phrase("draw(s)").then(AMOUNT).followedBy(phrase("card(s)")), DrawEffect::new);
}
