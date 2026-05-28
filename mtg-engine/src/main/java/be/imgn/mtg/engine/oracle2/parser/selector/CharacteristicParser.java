package be.imgn.mtg.engine.oracle2.parser.selector;

import com.google.common.labs.parse.Parser;

import be.imgn.mtg.engine.oracle2.domain.selector.CharacteristicSelector;

/// Dispatches over the implemented [CharacteristicSelector] arms.
///
/// Wired in: [be.imgn.mtg.engine.oracle2.domain.selector.CardTypeSelector],
/// [be.imgn.mtg.engine.oracle2.domain.selector.SubtypeSelector],
/// [be.imgn.mtg.engine.oracle2.domain.selector.SupertypeSelector],
/// [be.imgn.mtg.engine.oracle2.domain.selector.ColorSelector],
/// [be.imgn.mtg.engine.oracle2.domain.selector.NameSelector],
/// [be.imgn.mtg.engine.oracle2.domain.selector.ManaCostSelector],
/// [be.imgn.mtg.engine.oracle2.domain.selector.PowerSelector],
/// [be.imgn.mtg.engine.oracle2.domain.selector.ToughnessSelector].
///
/// [be.imgn.mtg.engine.oracle2.domain.selector.AbilitySelector] is
/// also a [CharacteristicSelector] arm but is **not** wired here —
/// its parser
/// ([be.imgn.mtg.engine.oracle2.parser.selector.AbilitySelectorParser])
/// returns [be.imgn.mtg.engine.oracle2.domain.selector.ObjectPropertySelector]
/// (not the narrower `CharacteristicSelector`) because compound
/// `with X or Y` forms produce
/// [ObjectPropertySelector.OneOf].
/// It enters the dispatch one layer up at
/// [PropertyParser#ATOMIC] instead.
///
/// Order: the more specific shares-with arms within Color/CardType/
/// Subtype/Name are tried inside their own parsers; here we simply
/// pick among the families in a longest-match-friendly order. Subtype
/// is tried last because the subtype keyword table is the largest and
/// most ambiguous.
public final class CharacteristicParser {
    private CharacteristicParser() {}

    /// Top-level [CharacteristicSelector] entry — union over the
    /// implemented arms. Name precedes the type axes because its
    /// distinctive prefixes ("named", "with the same name", "with the
    /// chosen name", "with no name") don't conflict with type
    /// keywords.
    public static final Parser<CharacteristicSelector> CHARACTERISTIC_SELECTOR = Parser.anyOf(
            NameSelectorParser.NAME_SELECTOR,
            ManaCostSelectorParser.MANA_COST_SELECTOR,
            PowerSelectorParser.POWER_SELECTOR,
            ToughnessSelectorParser.TOUGHNESS_SELECTOR,
            TypeSelectorParser.SUPERTYPE_SELECTOR,
            TypeSelectorParser.CARD_TYPE_SELECTOR,
            ColorSelectorParser.COLOR_SELECTOR,
            TypeSelectorParser.SUBTYPE_SELECTOR);
}
