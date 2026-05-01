package be.imgn.mtg.engine.oracle.domain;

/// Game object type references in oracle text (Rule 109.2).
public enum GameObjectType implements Parseable {
    PERMANENT("Permanent(s)"),
    SPELL("Spell(s)"),
    CARD("Card(s)"),
    TOKEN("Token(s)"),
    SOURCE("Source(s)"),
    ABILITY("[Ability|Abilities]"),
    /// Rule 109.5 — a player, admitted here so object selectors can
    /// uniformly target players ("Enchant player").
    PLAYER("Player(s)");

    private final String text;

    GameObjectType(String text) {
        this.text = text;
    }

    @Override
    public String text() {
        return text;
    }
}
