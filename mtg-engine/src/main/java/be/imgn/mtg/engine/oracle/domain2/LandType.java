package be.imgn.mtg.engine.oracle.domain2;

/// MTG land subtypes ({@mtg.rule 205.3i}). Sealed split into
/// [BasicLandType] (the five basic land types per {@mtg.rule 305.6})
/// and [NonBasicLandType] (every other printed land subtype). The
/// distinction matters because cards routinely refer specifically to
/// "basic land" or "basic land type" ({@mtg.rule 305.6}, fetch
/// lands, Coalition Victory).
public sealed interface LandType extends Subtype permits BasicLandType, NonBasicLandType {}
