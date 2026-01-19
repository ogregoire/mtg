package be.imgn.mtg.engine.ability.internal.parser.selector;

/// Represents a numeric amount in effect text.
public sealed interface Amount {

    /// An exact numeric value (e.g., "3 damage").
    record Exact(int value) implements Amount {}

    /// A variable reference by name (e.g., "that much" referring to a previous value).
    record Variable(String name) implements Amount {}

    /// The X value from the spell's cost or other source.
    record XValue() implements Amount {}
}
