package be.imgn.mtg.engine.characteristics;

import be.imgn.mtg.engine.characteristics.internal.DefaultCounters;

/// Counters on a permanent ({@mtg.rule 122}).
///
/// Counters are markers placed on objects that modify characteristics or interact with rules
/// and abilities. +1/+1 and -1/-1 counters modify power and toughness; loyalty counters track
/// planeswalker loyalty.
public interface Counters {

    /// Creates a new empty counters collection.
    ///
    /// @return a new empty counters collection
    static Counters create() {
        return DefaultCounters.create();
    }

    /// Returns the count of the specified counter type.
    ///
    /// @param counterType the type of counter to count
    /// @return the number of counters of that type
    int count(CounterType counterType);

    /// Adds counters of the specified type.
    ///
    /// @param counterType the type of counter to add
    /// @param count the number of counters to add
    void add(CounterType counterType, int count);

    /// Removes counters of the specified type.
    ///
    /// @param counterType the type of counter to remove
    /// @param count the number of counters to remove
    void remove(CounterType counterType, int count);

    /// Returns true if there are no counters.
    ///
    /// @return true if empty
    boolean isEmpty();
}
