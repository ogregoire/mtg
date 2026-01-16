package be.imgn.mtg.engine.characteristics;

/// Land subtypes, called land types ({@mtg.rule 205.3i}).
///
/// Land types are subtypes that are correlated to the land card type. Land types are divided
/// into basic land types (Plains, Island, Swamp, Mountain, Forest) and nonbasic land types.
public sealed interface LandType extends Subtype permits BasicLandType, NonBasicLandType {}
