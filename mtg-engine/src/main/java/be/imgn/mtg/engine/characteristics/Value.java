package be.imgn.mtg.engine.characteristics;

import be.imgn.mtg.engine.characteristics.internal.CounterValue;
import be.imgn.mtg.engine.characteristics.internal.FixedValue;

/// A value that can be queried, possibly dynamically.
public sealed interface Value permits FixedValue, CounterValue {

    /// Returns the current value.
    int value();

    /// Creates a fixed value.
    static Value of(int value) {
        return new FixedValue(value);
    }
}
