package be.imgn.mtg.engine.oracle2.parser.effect;

import static be.imgn.mtg.engine.oracle2.parser.Parsers.phrase;

import com.google.common.labs.parse.Parser;

import be.imgn.mtg.engine.oracle2.domain.effect.LegendRuleEffect;

/// Parser for [LegendRuleEffect] ({@mtg.rule 704.5j}). Recognises
/// the single fixed phrase `The "legend rule" doesn't apply.`
/// printed on Mirror Gallery. The literal `"` quote characters are
/// part of the phrase; the `phrase` template's word character class
/// doesn't admit `"`, so the quoted span is handled by
/// `parser.between("\"", "\"")`.
public final class LegendRuleEffectParser {
    private LegendRuleEffectParser() {}

    /// "The \"legend rule\" doesn't apply" — full sentence; period is
    /// consumed at the [EffectParser] level.
    public static final Parser<LegendRuleEffect> LEGEND_RULE = phrase("The")
            .followedBy(phrase("legend rule").between("\"", "\""))
            .followedBy(phrase("doesn't apply"))
            .thenReturn(LegendRuleEffect.DOES_NOT_APPLY);
}
