package be.imgn.mtg.engine.characteristics;

/// The supertypes in Magic ({@mtg.rule 205.4}).
///
/// A supertype is printed directly before the card type on the type line. The supertypes are
/// basic, legendary, snow, and world. Supertypes apply additional rules to the object.
public enum Supertype {
    BASIC,
    LEGENDARY,
    SNOW,
    WORLD
}
