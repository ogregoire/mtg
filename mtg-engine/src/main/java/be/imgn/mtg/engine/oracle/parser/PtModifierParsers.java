package be.imgn.mtg.engine.oracle.parser;

import static com.google.common.labs.parse.Parser.anyOf;
import static com.google.common.labs.parse.Parser.sequence;
import static com.google.common.labs.parse.Parser.string;
import static com.google.common.labs.parse.Parser.word;

import com.google.common.labs.parse.Parser;

import be.imgn.mtg.engine.oracle.domain.PtModifier;

/// Parsers for `±X/±Y` power/toughness modifier clauses. Produces
/// [PtModifier] values consumed by effect parsers such as
/// [EffectParsers#MODIFY_PT] and [EffectParsers#GAIN_ABILITY] tails.
final class PtModifierParsers {
    private PtModifierParsers() {}

    /// Standalone sign used by the `±X` (variable) component, since
    /// [AmountParsers#SIGNED_INT] requires digits to follow.
    private static final Parser<Integer> MOD_SIGN =
            anyOf(string("+").thenReturn(1), string("-").thenReturn(-1));

    /// One side of a P/T modifier — either `±X` (a
    /// [PtModifier.Component.Variable]) or a signed integer (a
    /// [PtModifier.Component.Fixed]). The `±X` branch is tried first
    /// so `+X` isn't misread as a numeric amount.
    private static final Parser<PtModifier.Component> PT_COMPONENT = anyOf(
            sequence(MOD_SIGN, word("X"), (sign, _) -> new PtModifier.Component.Variable(sign)),
            AmountParsers.SIGNED_INT.map(PtModifier.Component.Fixed::new));

    /// A P/T modifier, optionally preceded by a "twice" / "N times"
    /// multiplier (Nuclear Fallout: "Each creature gets twice -X/-X").
    /// The multiplier scales the magnitude at resolution time; it's
    /// distinct from a "for each X" count-of scaleBy tail on the
    /// enclosing [ModifyPT][Effect.ModifyPT].
    static final Parser<PtModifier> PT_MODIFIER = anyOf(
            sequence(
                    anyOf(word("twice").thenReturn(2), AmountParsers.NUMBER.followedBy(word("times"))),
                    PT_COMPONENT,
                    string("/").then(PT_COMPONENT),
                    (mult, p, t) -> new PtModifier(p, t, mult)),
            sequence(PT_COMPONENT, string("/").then(PT_COMPONENT), PtModifier::new));
}
