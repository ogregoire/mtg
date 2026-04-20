package be.imgn.mtg.engine.oracle;

/// MTG battle subtypes (Rule 205.3q).
public enum BattleType implements Subtype {
    SIEGE("Siege(s)");

    private final String text;

    BattleType(String text) {
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
