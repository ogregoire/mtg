package be.imgn.mtg.engine.object;

/// A source that can be cast as a [Spell].
///
/// This sealed interface represents objects that can be cast:
/// - [Card]: A card being cast from hand or another zone
/// - [CardCopy]: A copy of a card created by an effect
public sealed interface SpellSource permits Card, CardCopy {}
