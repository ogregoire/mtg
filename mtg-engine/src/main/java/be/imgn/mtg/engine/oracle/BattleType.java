package be.imgn.mtg.engine.oracle;

/// MTG battle subtypes (Rule 205.3q).
public enum BattleType {
    SIEGE("Siege");

    private final String text;

    BattleType(String text) {
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
