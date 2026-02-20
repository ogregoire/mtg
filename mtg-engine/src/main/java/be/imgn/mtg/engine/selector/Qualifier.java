package be.imgn.mtg.engine.selector;

/// Qualifiers that modify the selection criteria for objects.
public sealed interface Qualifier {

    /// Indicates the object must be targeted.
    record Target() implements Qualifier {}

    /// Indicates the object must have a trait (e.g., "legendary", "blue").
    record Has(Trait trait) implements Qualifier {}

    /// Indicates the object must NOT have a trait (e.g., "nonland", "nonblack").
    record Not(Trait trait) implements Qualifier {}

    /// Indicates the object must have a certain status.
    record Status(StatusType status) implements Qualifier {}
}
