package be.imgn.mtg.engine.oracle.domain;

/// MTG card types (Rule 205).
public enum CardType implements Parseable {
    CREATURE("Creature(s)"),
    ARTIFACT("Artifact(s)"),
    ENCHANTMENT("Enchantment(s)"),
    LAND("Land(s)"),
    PLANESWALKER("Planeswalker(s)"),
    BATTLE("Battle(s)"),
    INSTANT("Instant(s)"),
    SORCERY("[Sorcery|Sorceries]"),
    KINDRED("Kindred"),
    DUNGEON("Dungeon(s)");

    private final String text;

    CardType(String text) {
        this.text = text;
    }

    @Override
    public String text() {
        return text;
    }
}
