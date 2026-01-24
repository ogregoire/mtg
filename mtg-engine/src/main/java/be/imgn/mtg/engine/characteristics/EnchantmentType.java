package be.imgn.mtg.engine.characteristics;

/// Enchantment subtypes, called enchantment types ({@mtg.rule 205.3h}).
///
/// Enchantment types are subtypes that are correlated to the enchantment card type. An
/// enchantment can have multiple enchantment types. Aura, Saga, and Class have special rules.
public enum EnchantmentType implements Subtype {
    /// The Aura enchantment type.
    AURA,
    /// The Background enchantment type.
    BACKGROUND,
    /// The Cartouche enchantment type.
    CARTOUCHE,
    /// The Class enchantment type.
    CLASS,
    /// The Curse enchantment type.
    CURSE,
    /// The Role enchantment type.
    ROLE,
    /// The Rune enchantment type.
    RUNE,
    /// The Saga enchantment type.
    SAGA,
    /// The Shard enchantment type.
    SHARD,
    /// The Shrine enchantment type.
    SHRINE
}
