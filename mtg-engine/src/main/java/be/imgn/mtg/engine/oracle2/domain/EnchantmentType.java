package be.imgn.mtg.engine.oracle2.domain;

/// MTG enchantment subtypes (Rule 205.3h).
public enum EnchantmentType implements Subtype {
    AURA("Aura(s)"),
    BACKGROUND("Background(s)"),
    CARTOUCHE("Cartouche(s)"),
    CASE("Case(s)"),
    CLASS("Class(es)"),
    CURSE("Curse(s)"),
    ROLE("Role(s)"),
    ROOM("Room(s)"),
    RUNE("Rune(s)"),
    SAGA("Saga(s)"),
    SHARD("Shard(s)"),
    SHRINE("Shrine(s)");

    private final String text;

    EnchantmentType(String text) {
        this.text = text;
    }

    @Override
    public String text() {
        return text;
    }

    @Override
    public String toString() {
        return texts().getFirst();
    }
}
