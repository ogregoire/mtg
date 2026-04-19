package be.imgn.mtg.engine.oracle;

import java.util.List;

/// MTG enchantment subtypes (Rule 205.3h).
public enum EnchantmentType implements Subtype {
    AURA("Aura"),
    BACKGROUND("Background"),
    CARTOUCHE("Cartouche"),
    CASE("Case"),
    CLASS("Class", "Classes"),
    CURSE("Curse"),
    ROLE("Role"),
    ROOM("Room"),
    RUNE("Rune"),
    SAGA("Saga"),
    SHARD("Shard"),
    SHRINE("Shrine");

    private final String text;
    private final List<String> texts;

    EnchantmentType(String text) {
        this(text, text + "s");
    }

    EnchantmentType(String text, String plural) {
        this.text = text;
        this.texts = plural.equals(text) ? List.of(text) : List.of(text, plural);
    }

    public String text() {
        return text;
    }

    @Override
    public List<String> texts() {
        return texts;
    }

    @Override
    public String toString() {
        return text;
    }
}
