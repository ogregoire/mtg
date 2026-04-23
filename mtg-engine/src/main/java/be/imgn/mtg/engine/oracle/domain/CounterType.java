package be.imgn.mtg.engine.oracle.domain;

/// A counter type (e.g., +1/+1, loyalty, charge).
public sealed interface CounterType {
    record PtCounter(int power, int toughness) implements CounterType {}

    record Named(String name) implements CounterType {}

    /// Creates a [PtCounter] counter type.
    static CounterType ptCounter(int power, int toughness) {
        return new PtCounter(power, toughness);
    }

    /// Creates a [Named] counter type.
    static CounterType named(String name) {
        return new Named(name);
    }
}
