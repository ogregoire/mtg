package be.imgn.mtg.engine.oracle2.parser.effect;

import static be.imgn.mtg.engine.oracle2.parser.Parsers.phrase;

import java.util.function.Function;

import com.google.common.labs.parse.Parser;

import be.imgn.mtg.engine.oracle2.domain.effect.Effect;
import be.imgn.mtg.engine.oracle2.domain.effect.SkipStepEffect;
import be.imgn.mtg.engine.oracle2.domain.selector.Selector;
import be.imgn.mtg.engine.turn.Step;

/// Parser for [SkipStepEffect] ({@mtg.rule 500.8}). Subject-led:
/// "(who) skip(s) their (step) step(s)." Today only the upkeep
/// step lands; new step phrases ("draw step", "end step", …) get
/// new arms when cards demand them. The "their" pronoun
/// back-references the subject — left implicit in the AST since
/// `who` already names the player.
public final class SkipStepEffectParser {
    private SkipStepEffectParser() {}

    /// "skip(s) their upkeep step(s)" — subject-led wrapper.
    /// Hardcoded to [Step#UPKEEP] until other step variants appear.
    public static final Parser<Function<Selector, Effect>> SKIPS_STEP_FN = EffectParser.subjectVerb(
            phrase("skip(s) their upkeep step(s)").thenReturn(Step.UPKEEP), SkipStepEffect::new);
}
