package be.imgn.mtg.engine.ability.internal.parser.selector;

/// Represents game zones in Magic: The Gathering ({@mtg.rule 400}).
public enum Zone {
    /// The zone where permanents exist.
    BATTLEFIELD,
    /// A player's hand of cards.
    HAND,
    /// A player's discard pile.
    GRAVEYARD,
    /// A player's deck.
    LIBRARY,
    /// The zone for exiled cards.
    EXILE,
    /// The zone where spells and abilities exist while resolving.
    STACK,
    /// The zone for commanders and other special cards.
    COMMAND
}
