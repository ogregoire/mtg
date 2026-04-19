package be.imgn.mtg.engine.oracle;

import java.util.List;

/// MTG battle subtypes (Rule 205.3q).
public enum BattleType implements Subtype {
    SIEGE("Siege");

    private final String text;
    private final List<String> texts;

    BattleType(String text) {
        this(text, text + "s");
    }

    BattleType(String text, String plural) {
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
