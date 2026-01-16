package be.imgn.mtg.engine.characteristics;

/// Land subtypes (rule 205.3i).
public sealed interface LandType extends Subtype permits BasicLandType, NonBasicLandType {}
