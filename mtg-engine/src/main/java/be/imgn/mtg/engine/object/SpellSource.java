package be.imgn.mtg.engine.object;

/// A source that can be cast as a spell.
public sealed interface SpellSource permits Card, CardCopy {}
