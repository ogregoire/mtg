package be.imgn.mtg.engine.oracle;

import static com.google.common.labs.parse.Parser.anyOf;
import static com.google.common.labs.parse.Parser.sequence;
import static com.google.common.labs.parse.Parser.string;
import static com.google.common.labs.parse.Parser.word;

import com.google.common.labs.parse.Parser;

/// Parsers for `±X/±Y` power/toughness modifier clauses. Produces
/// [PtModifier] values consumed by effect parsers such as
/// [EffectParsers#MODIFY_PT] and [EffectParsers#GAIN_ABILITY] tails.
final class PtModifierParsers {
    private PtModifierParsers() {}

    private static final Parser<Integer> MOD_SIGN =
            anyOf(string("+").thenReturn(1), string("-").thenReturn(-1));

    /// One side of a P/T modifier — either `±X` (a
    /// [PtModifier.Component.Variable]) or a signed integer (a
    /// [PtModifier.Component.Fixed]). The `±X` branch is tried first
    /// so `+X` isn't misread as a numeric amount.
    private static final Parser<PtModifier.Component> PT_COMPONENT = anyOf(
            sequence(MOD_SIGN, word("X"), (sign, _) -> (PtModifier.Component) new PtModifier.Component.Variable(sign)),
            sequence(MOD_SIGN, SelectorParsers.INTEGER, (sign, value) ->
                    (PtModifier.Component) new PtModifier.Component.Fixed(sign * value)));

    static final Parser<PtModifier> PT_MODIFIER =
            sequence(PT_COMPONENT, string("/").then(PT_COMPONENT), PtModifier::new);
}
