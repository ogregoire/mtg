package be.imgn.mtg.engine.characteristics;

/// The five basic land types ({@mtg.rule 305.6}).
///
/// The basic land types are Plains, Island, Swamp, Mountain, and Forest. Each basic land type
/// has an intrinsic ability to produce one mana of the corresponding color. Any land with a
/// basic land type has that ability.
public enum BasicLandType implements LandType {
    /// Plains, produces white mana.
    PLAINS,
    /// Island, produces blue mana.
    ISLAND,
    /// Swamp, produces black mana.
    SWAMP,
    /// Mountain, produces red mana.
    MOUNTAIN,
    /// Forest, produces green mana.
    FOREST
}
