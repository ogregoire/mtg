package be.imgn.mtg.engine.characteristics;

/// Enchantment subtypes (rule 205.3h).
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
