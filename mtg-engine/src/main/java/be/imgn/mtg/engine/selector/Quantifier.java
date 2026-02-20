package be.imgn.mtg.engine.selector;

/// Represents quantifiers in oracle text that specify how many objects are affected.
public sealed interface Quantifier {

    /// Exactly one object.
    record One() implements Quantifier {}

    /// A specific count of objects.
    record Count(int value) implements Quantifier {}

    /// All matching objects.
    record All() implements Quantifier {}

    /// Each matching object (similar to all, but emphasizes individual processing).
    record Each() implements Quantifier {}

    /// Up to a maximum number of objects.
    record UpTo(int max) implements Quantifier {}

    /// Any number of objects (player's choice).
    record Any() implements Quantifier {}

    /// Another object (not the source or previously referenced object).
    record Another() implements Quantifier {}
}
