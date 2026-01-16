package be.imgn.mtg.engine.characteristics;

import be.imgn.mtg.engine.characteristics.internal.DefaultCounters;

/// Counters on a permanent ({@mtg.rule 122}).
///
/// Counters are markers placed on objects that modify characteristics or interact with rules
/// and abilities. +1/+1 and -1/-1 counters modify power and toughness; loyalty counters track
/// planeswalker loyalty.
public interface Counters {

    /// Creates a new empty counters collection.
    static Counters create() {
        return DefaultCounters.create();
    }

    /// Returns the count of the specified counter type.
    int count(CounterType counterType);

    /// Adds counters of the specified type.
    void add(CounterType counterType, int count);

    /// Removes counters of the specified type.
    void remove(CounterType counterType, int count);

    /// Returns true if there are no counters.
    boolean isEmpty();
}
