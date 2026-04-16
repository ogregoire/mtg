package be.imgn.mtg.engine.oracle;

/// MTG enchantment subtypes (Rule 205.3h).
public enum EnchantmentType {
    AURA("Aura"),
    BACKGROUND("Background"),
    CARTOUCHE("Cartouche"),
    CASE("Case"),
    CLASS("Class"),
    CURSE("Curse"),
    ROLE("Role"),
    ROOM("Room"),
    RUNE("Rune"),
    SAGA("Saga"),
    SHARD("Shard"),
    SHRINE("Shrine");

    private final String text;

    EnchantmentType(String text) {
        this.text = text;
    }

    public String text() {
        return text;
    }

    @Override
    public String toString() {
        return text;
    }
}
