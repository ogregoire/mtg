package be.imgn.mtg.engine.oracle.domain2;

/// MTG supertypes (Rule 205.4).
public enum Supertype implements Parseable {
    LEGENDARY("Legendary"),
    BASIC("Basic"),
    SNOW("Snow"),
    WORLD("World");

    private final String text;

    Supertype(String text) {
        this.text = text;
    }

    @Override
    public String text() {
        return text;
    }
}
