package be.imgn.mtg.engine.characteristics;

/// The supertypes in Magic ({@mtg.rule 205.4}).
///
/// A supertype is printed directly before the card type on the type line. The supertypes are
/// basic, legendary, snow, and world. Supertypes apply additional rules to the object.
public enum Supertype {
    /// Basic supertype, used primarily on basic lands.
    BASIC,
    /// Legendary supertype, subject to the legend rule.
    LEGENDARY,
    /// Snow supertype, can produce snow mana.
    SNOW,
    /// World supertype, subject to the world rule.
    WORLD
}
