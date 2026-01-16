package be.imgn.mtg.engine.object;

/// An object that can be cast as a [Spell] ({@mtg.rule 112}).
///
/// To cast a spell is to take a card or copy from where it is and put it on the stack.
/// Cards are typically cast from the hand, but some effects allow casting from other zones.
/// Card copies created by effects can also be cast.
///
/// A spell retains a reference to its source for tracking purposes and for effects that
/// reference "this spell" or the card it came from.
public sealed interface SpellSource permits Card, CardCopy {}
