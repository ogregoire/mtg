package be.imgn.mtg.engine.characteristics;

import be.imgn.mtg.engine.characteristics.internal.CounterValue;
import be.imgn.mtg.engine.characteristics.internal.FixedValue;

/// A numeric value characteristic such as power, toughness, or loyalty ({@mtg.rule 208}).
///
/// Power and toughness are characteristics only creatures have. Loyalty is a characteristic
/// only planeswalkers have. These values may be fixed or derived from counters or effects.
public sealed interface Value permits FixedValue, CounterValue {

    /// Returns the current value.
    int value();

    /// Creates a fixed value.
    static Value of(int value) {
        return new FixedValue(value);
    }
}
