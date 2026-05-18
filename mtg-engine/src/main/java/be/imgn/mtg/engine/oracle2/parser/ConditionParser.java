package be.imgn.mtg.engine.oracle2.parser;

import static be.imgn.mtg.engine.oracle2.parser.AmountMatcherParser.AMOUNT_MATCHER;
import static be.imgn.mtg.engine.oracle2.parser.Parsers.phrase;
import static com.google.common.labs.parse.Parser.anyOf;
import static com.google.common.labs.parse.Parser.sequence;

import java.util.List;

import com.google.common.labs.parse.Parser;

import be.imgn.mtg.engine.oracle2.domain.Amount;
import be.imgn.mtg.engine.oracle2.domain.Condition;
import be.imgn.mtg.engine.oracle2.domain.selector.ObjectPropertySelector;
import be.imgn.mtg.engine.oracle2.domain.selector.ObjectTypeSelector;
import be.imgn.mtg.engine.oracle2.domain.selector.QuantifierSelector;
import be.imgn.mtg.engine.oracle2.domain.selector.Selector;
import be.imgn.mtg.engine.oracle2.domain.selector.ZoneSelector;
import be.imgn.mtg.engine.oracle2.parser.selector.CharacteristicParser;
import be.imgn.mtg.engine.oracle2.parser.selector.SelectorParser;

/// Parser for [Condition] — the predicate body of an intervening-if
/// clause ({@mtg.rule 603.4}). The leading `if` and trailing comma
/// are owned by
/// [be.imgn.mtg.engine.oracle2.parser.ability.AbilityParser]; this
/// parser handles only the predicate text between them.
///
/// Two arms today: [#HAS_LIFE] for `<selector> [has|have] <matcher>
/// life` (Felidar Sovereign, Test of Endurance) and [#CONTROLS] for
/// `<selector> control(s) <matcher> <selector>` (the gate of mana-
/// replacement riders like "If you control four or more creatures").
/// Future condition shapes get added as new private parsers and
/// folded into [#CONDITION]'s `anyOf`.
public final class ConditionParser {
    private ConditionParser() {}

    /// `<selector> [has|have] <matcher> life` — [Condition.HasLife].
    private static final Parser<Condition.HasLife> HAS_LIFE = sequence(
            SelectorParser.SELECTOR,
            phrase("[has|have]").then(AMOUNT_MATCHER).followedBy(phrase("life")),
            Condition.HasLife::new);

    /// Narrow "what" parser for [Condition.Controls] — accepts a bare
    /// `[a|an]? <characteristic>+` (no zone clause, no comma-absorbing
    /// `PROPERTY` chain). Built this way because the standard
    /// `SelectorParser.SELECTOR` (and `bareSelectorWith`) routes
    /// through `PropertyParser.PROPERTY`, whose
    /// `OR_GROUP.optionallyFollowedBy(",").atLeastOnce()` greedily
    /// consumes the trailing comma of the enclosing "If \<cond\>, …"
    /// clause and breaks the parse downstream. The wrapping shape
    /// (`QuantifierSelector(Exact(1), Battlefield(Permanent(...)))`)
    /// is rebuilt manually to match what `SELECTOR` would have emitted
    /// for the same input.
    private static final Parser<Selector> CONTROLS_WHAT = phrase("a(n)")
            .optional()
            .then(CharacteristicParser.CHARACTERISTIC_SELECTOR
                    .<ObjectPropertySelector>map(c -> c)
                    .atLeastOnce())
            .map(ConditionParser::wrapControlsWhat);

    /// `<who> control(s) <count> <what>` — [Condition.Controls]. Uses
    /// [#CONTROLS_WHAT] for the `<what>` slot — see its doc for why
    /// the standard selector parser can't be used here.
    private static final Parser<Condition.Controls> CONTROLS = sequence(
            SelectorParser.SELECTOR, phrase("control(s)").then(AMOUNT_MATCHER), CONTROLS_WHAT, Condition.Controls::new);

    /// Build the `QuantifierSelector(Exact(1), Battlefield(Permanent(...)))`
    /// envelope around the parsed property atoms.
    private static Selector wrapControlsWhat(List<ObjectPropertySelector> atoms) {
        ObjectPropertySelector property =
                atoms.size() == 1 ? atoms.getFirst() : new ObjectPropertySelector.AllOf(atoms);
        return new QuantifierSelector(
                new Amount.Exact(1), new ZoneSelector.Battlefield(new ObjectTypeSelector.Permanent(property)));
    }

    /// Top-level [Condition]. Order: longer-prefix `HAS_LIFE` first
    /// (the "[has|have] N life" tail is distinctive); `CONTROLS`
    /// second.
    public static final Parser<Condition> CONDITION = anyOf(HAS_LIFE, CONTROLS);
}
