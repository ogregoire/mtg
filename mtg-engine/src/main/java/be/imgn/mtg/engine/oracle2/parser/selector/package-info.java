/// Selector parsers for `oracle2.domain.selector`. Built on top of
/// `oracle2.parser` shared utilities ([be.imgn.mtg.engine.oracle2.parser.Parsers],
/// [be.imgn.mtg.engine.oracle2.parser.AmountParser]).
///
/// File organization mirrors the domain selector hierarchy:
///
/// - [SelectorParser] — top-level [Selector][be.imgn.mtg.engine.oracle2.domain.selector.Selector]
///   entry; wires the [Refs] forward-decls.
/// - [PlayerSelectorParser] — every [PlayerSelector][be.imgn.mtg.engine.oracle2.domain.selector.PlayerSelector]
///   arm.
/// - [ObjectSelectorParser] — `~` self-reference + [ZoneParser] dispatch.
/// - [ZoneParser] — the 7 zone arms with their nested `Contents`.
/// - [ObjectTypeParser] — the 7 object-class arms (Permanent, Token,
///   Spell, …).
/// - [PropertyParser] — [ObjectPropertySelector][be.imgn.mtg.engine.oracle2.domain.selector.ObjectPropertySelector]
///   composition (AllOf, AnyOf, Not) + concrete property records +
///   negation.
/// - [CharacteristicParser] — [CharacteristicSelector][be.imgn.mtg.engine.oracle2.domain.selector.CharacteristicSelector]
///   dispatch among the 4 implemented arms.
/// - [TypeSelectorParser] — CardType + Subtype + Supertype keyword tables.
/// - [ColorSelectorParser] — [ColorSelector][be.imgn.mtg.engine.oracle2.domain.selector.ColorSelector]
///   arms.
/// - [QuantifierParser] — [Quantifier][be.imgn.mtg.engine.oracle2.domain.Quantifier]
///   parser ("select how many?").
/// - [Refs] — forward-declared [com.google.common.labs.parse.Parser.Rule]
///   handles for the recursive selector axes.
///
/// **Hard rule**: nothing in this package may import from
/// `be.imgn.mtg.engine.oracle.domain` or
/// `be.imgn.mtg.engine.oracle.parser` — the two parser trees stay
/// fully isolated.
///
/// **Reported gaps** — selectors with bare-marker domain shapes
/// that cannot be parsed yet: `AbilitySelector`. The highest-impact
/// remaining gap (every "with flying"-style filter); needs a
/// separate `Ability` domain primitive first.
package be.imgn.mtg.engine.oracle2.parser.selector;
