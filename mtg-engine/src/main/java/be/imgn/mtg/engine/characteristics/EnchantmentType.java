package be.imgn.mtg.engine.characteristics;

/// Enchantment subtypes, called enchantment types ({@mtg.rule 205.3h}).
///
/// Enchantment types are subtypes that are correlated to the enchantment card type. An
/// enchantment can have multiple enchantment types. Aura, Saga, and Class have special rules.
public enum EnchantmentType implements Subtype {
    AURA("Aura"),
    BACKGROUND("Background"),
    CARTOUCHE("Cartouche"),
    CLASS("Class"),
    CURSE("Curse"),
    ROLE("Role"),
    RUNE("Rune"),
    SAGA("Saga"),
    SHARD("Shard"),
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
