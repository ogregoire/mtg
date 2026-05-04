package be.imgn.mtg.engine.oracle2.parser.selector;

import static be.imgn.mtg.engine.oracle2.parser.AmountMatcherParser.AMOUNT_MATCHER;
import static be.imgn.mtg.engine.oracle2.parser.Parsers.orList;
import static be.imgn.mtg.engine.oracle2.parser.Parsers.phrase;
import static be.imgn.mtg.engine.oracle2.parser.selector.ManaCostSelectorParser.MANA_VALUE_ASPECT;
import static be.imgn.mtg.engine.oracle2.parser.selector.PowerSelectorParser.POWER_ASPECT;
import static be.imgn.mtg.engine.oracle2.parser.selector.ToughnessSelectorParser.TOUGHNESS_ASPECT;
import static com.google.common.labs.parse.Parser.anyOf;
import static com.google.common.labs.parse.Parser.sequence;

import java.util.List;
import java.util.function.Function;

import com.google.common.labs.parse.Parser;

import be.imgn.mtg.engine.oracle2.domain.AmountMatcher;
import be.imgn.mtg.engine.oracle2.domain.selector.ObjectPropertySelector;

/// Generic parser for numeric-aspect comparisons, including
/// shared-matcher disjunctions: "with power or toughness 1 or less"
/// distributes the `1 or less` matcher across both axes, producing
/// `AnyOf(HasPower(AtMost(1)), HasToughness(AtMost(1)))`.
///
/// Composition only — each per-axis parser owns its keyword and
/// wrapping function, exposed as a package-private `*_ASPECT` field
/// (see [PowerSelectorParser#POWER_ASPECT],
/// [ToughnessSelectorParser#TOUGHNESS_ASPECT],
/// [ManaCostSelectorParser#MANA_VALUE_ASPECT]). This file just
/// composes those into the shared "with [aspects] [matcher]" shape.
///
/// A single-element list returns the wrapped result directly; a
/// multi-element list wraps in [ObjectPropertySelector.AnyOf].
public final class NumericAspectParser {
    private NumericAspectParser() {}

    /// Composed aspect dispatch — one entry per numeric axis,
    /// owned by the axis parser.
    private static final Parser<Function<AmountMatcher, ObjectPropertySelector>> ASPECT =
            anyOf(POWER_ASPECT, TOUGHNESS_ASPECT, MANA_VALUE_ASPECT);

    /// "with [aspect-list joined by 'or'] [matcher]" — single-aspect
    /// (`with power 3 or greater`) and multi-aspect
    /// (`with power or toughness 1 or less`) cases handled
    /// uniformly. Single → direct wrap; multiple →
    /// [ObjectPropertySelector.AnyOf].
    public static final Parser<ObjectPropertySelector> NUMERIC_ASPECT =
            sequence(phrase("with").then(orList(ASPECT)), AMOUNT_MATCHER, NumericAspectParser::distribute);

    private static ObjectPropertySelector distribute(
            List<Function<AmountMatcher, ObjectPropertySelector>> aspects, AmountMatcher matcher) {
        if (aspects.size() == 1) {
            return aspects.getFirst().apply(matcher);
        }
        return new ObjectPropertySelector.AnyOf(
                aspects.stream().map(f -> f.apply(matcher)).toList());
    }
}
