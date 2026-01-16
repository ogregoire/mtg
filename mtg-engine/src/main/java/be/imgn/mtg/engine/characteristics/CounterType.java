package be.imgn.mtg.engine.characteristics;

/// Common counter types in Magic.
public enum CounterType {
    LOYALTY("loyalty"),
    PLUS_ONE_PLUS_ONE("+1/+1"),
    MINUS_ONE_MINUS_ONE("-1/-1");

    private final String text;

    CounterType(String text) {
        this.text = text;
    }

    public String text() {
        return text;
    }
}
