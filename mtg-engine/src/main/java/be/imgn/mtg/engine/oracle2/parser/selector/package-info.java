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
///   composition (AllOf, OneOf, Not) + concrete property records +
///   negation.
/// - [CharacteristicParser] — [CharacteristicSelector][be.imgn.mtg.engine.oracle2.domain.selector.CharacteristicSelector]
///   dispatch among the implemented arms.
/// - [TypeSelectorParser] — CardType + Subtype + Supertype keyword tables.
/// - [ColorSelectorParser] — [ColorSelector][be.imgn.mtg.engine.oracle2.domain.selector.ColorSelector]
///   arms.
/// - [NameSelectorParser] — [NameSelector][be.imgn.mtg.engine.oracle2.domain.selector.NameSelector]
///   arms ("named X", "with the same name as X", …).
/// - [ManaCostSelectorParser], [PowerSelectorParser],
///   [ToughnessSelectorParser] — the numeric-axis characteristic
///   selectors; each also exposes a package-private `*_ASPECT` for
///   [NumericAspectParser].
/// - [NumericAspectParser] — shared "with [aspects] [matcher]"
///   composition over the numeric axes.
/// - [AbilitySelectorParser] — "with"/"without" keyword filters.
/// - [ObjectCounterSelectorParser], [PlayerCounterSelectorParser] —
///   counter-presence predicates on objects and players.
/// - [StatusSelectorParser] — tapped / flipped / phased status arms.
/// - [StickerSelectorParser] — the "stickered" adjective.
/// - [QuantifierParser] — [Quantifier][be.imgn.mtg.engine.oracle2.domain.Quantifier]
///   parser ("select how many?").
/// - [Refs] — forward-declared [com.google.common.labs.parse.Parser.Rule]
///   handles for the recursive selector axes.
///
/// **Hard rule**: nothing in this package may import from
/// `be.imgn.mtg.engine.oracle.domain` or
/// `be.imgn.mtg.engine.oracle.parser` — the two parser trees stay
/// fully isolated.
package be.imgn.mtg.engine.oracle2.parser.selector;
