package be.imgn.mtg.engine.characteristics.internal;

import be.imgn.mtg.engine.characteristics.CounterType;
import be.imgn.mtg.engine.characteristics.Counters;
import be.imgn.mtg.engine.characteristics.Value;

/// A value derived from the count of a specific counter type.
public record CounterValue(Counters counters, CounterType counterType) implements Value {

    @Override
    public int value() {
        return counters.count(counterType);
    }
}
