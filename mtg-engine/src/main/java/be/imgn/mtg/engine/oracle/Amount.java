package be.imgn.mtg.engine.oracle;

/// Numeric amount in oracle text.
public sealed interface Amount {
    record Exact(int value) implements Amount {}

    record Variable() implements Amount {}

    record Reference(String type) implements Amount {}

    record Formula(String expression) implements Amount {}

    /// Creates an {@link Exact} amount.
    static Amount exact(int value) {
        return new Exact(value);
    }

    /// Creates a {@link Variable} amount.
    static Amount variable() {
        return new Variable();
    }

    /// Creates a {@link Reference} amount.
    static Amount reference(String type) {
        return new Reference(type);
    }

    /// Creates a {@link Formula} amount.
    static Amount formula(String expression) {
        return new Formula(expression);
    }
}
