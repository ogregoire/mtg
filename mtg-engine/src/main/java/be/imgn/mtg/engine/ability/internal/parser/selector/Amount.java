package be.imgn.mtg.engine.ability.internal.parser.selector;

/// Represents a numeric amount in effect text.
public sealed interface Amount {

    /// Convenience constant for the X value.
    Variable X = Variable.X;

    /// An exact numeric value (e.g., "3 damage").
    record Exact(int value) implements Amount {}

    /// A reference to a previously defined value (e.g., "that much" referring to a previous value).
    record Reference(String name) implements Amount {}

    /// The X value from the spell's cost or other source.
    enum Variable implements Amount {
        X
    }
}
