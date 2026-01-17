package be.imgn.mtg.engine.characteristics;

/// Enchantment subtypes, called enchantment types ({@mtg.rule 205.3h}).
///
/// Enchantment types are subtypes that are correlated to the enchantment card type. An
/// enchantment can have multiple enchantment types. Aura, Saga, and Class have special rules.
public enum EnchantmentType implements Subtype {
    /// The Aura enchantment type.
    AURA("Aura"),
    /// The Background enchantment type.
    BACKGROUND("Background"),
    /// The Cartouche enchantment type.
    CARTOUCHE("Cartouche"),
    /// The Class enchantment type.
    CLASS("Class"),
    /// The Curse enchantment type.
    CURSE("Curse"),
    /// The Role enchantment type.
    ROLE("Role"),
    /// The Rune enchantment type.
    RUNE("Rune"),
    /// The Saga enchantment type.
    SAGA("Saga"),
    /// The Shard enchantment type.
    SHARD("Shard"),
    /// The Shrine enchantment type.
    SHRINE("Shrine");

    private final String text;

    EnchantmentType(String text) {
        this.text = text;
    }

    @Override
    public String text() {
        return text;
    }
}
