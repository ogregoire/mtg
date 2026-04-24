package be.imgn.mtg.engine.oracle.domain;

/// Named characteristic of a player or game object that oracle text
/// references numerically ("target creature's power", "your life total",
/// "the lowest mana value among …"). The closed enum replaces the free-text
/// `String property` slot used in earlier iterations of [Amount.PropertyOf],
/// [Amount.Extremum], [Effect.SetPropertyValue],
/// [Effect.ExchangeLifeWithProperty], and [Effect.CrewsUsing]. Not every
/// effect accepts every value — each parser constrains its valid subset.
public enum Property {
    POWER,
    TOUGHNESS,
    /// "strength" — legacy/alternative characteristic name; retained because
    /// a handful of cards (and test fixtures) still emit it.
    STRENGTH,
    LIFE_TOTAL,
    MANA_VALUE,
    HAND_SIZE
}
