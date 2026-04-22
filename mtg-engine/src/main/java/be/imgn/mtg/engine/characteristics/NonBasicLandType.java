package be.imgn.mtg.engine.characteristics;

/// Nonbasic land types ({@mtg.rule 205.3i}).
///
/// These are land types other than the five basic land types. Unlike basic land types, these
/// do not have intrinsic mana abilities. Examples include Cave, Desert, Gate, Lair, and Locus.
public enum NonBasicLandType implements LandType {
    /// The Cave nonbasic land type.
    CAVE,
    /// The Desert nonbasic land type.
    DESERT,
    /// The Gate nonbasic land type.
    GATE,
    /// The Lair nonbasic land type.
    LAIR,
    /// The Locus nonbasic land type.
    LOCUS,
    /// The Mine nonbasic land type.
    MINE,
    /// The Planet nonbasic land type.
    PLANET,
    /// The Power-Plant nonbasic land type.
    POWER_PLANT,
    /// The Sphere nonbasic land type.
    SPHERE,
    /// The Tower nonbasic land type.
    TOWER,
    /// The Town nonbasic land type.
    TOWN,
    /// The Urza's nonbasic land type.
    URZAS
}
