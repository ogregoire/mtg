package be.imgn.mtg.engine.characteristics.internal;

import be.imgn.mtg.engine.characteristics.CounterType;
import be.imgn.mtg.engine.characteristics.Counters;
import be.imgn.mtg.engine.util.Multiset;

/// Default implementation of Counters.
public final class DefaultCounters implements Counters {

    private final Multiset<CounterType> counters;

    private DefaultCounters(Multiset<CounterType> counters) {
        this.counters = counters;
    }

    public static Counters create() {
        return new DefaultCounters(Multiset.newHashMultiset());
    }

    @Override
    public int count(CounterType counterType) {
        return counters.count(counterType);
    }

    @Override
    public void add(CounterType counterType, int count) {
        counters.add(counterType, count);
    }

    @Override
    public void remove(CounterType counterType, int count) {
        counters.remove(counterType, count);
    }

    @Override
    public boolean isEmpty() {
        return counters.isEmpty();
    }
}
