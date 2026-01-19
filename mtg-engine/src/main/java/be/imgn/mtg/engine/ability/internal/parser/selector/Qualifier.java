package be.imgn.mtg.engine.ability.internal.parser.selector;

import be.imgn.mtg.engine.characteristics.Supertype;

/// Qualifiers that modify the selection criteria for objects.
public sealed interface Qualifier {

    /// Indicates the object must be targeted.
    record Target() implements Qualifier {}

    /// Indicates the object must NOT match a certain type/color.
    record Negation(NegationType type) implements Qualifier {}

    /// Indicates the object must have a certain status.
    record Status(StatusType status) implements Qualifier {}

    /// Indicates the object must have a certain supertype.
    record SupertypeQualifier(Supertype supertype) implements Qualifier {}
}
