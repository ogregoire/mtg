package be.imgn.mtg.engine.characteristics;

import static be.imgn.mtg.engine.assertions.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CountersTest {

    @Test
    void newCountersAreEmpty() {
        var counters = Counters.create();

        assertThat(counters).isEmpty().hasCount(CounterType.LOYALTY, 0).hasCount(CounterType.PLUS_ONE_PLUS_ONE, 0);
    }

    @Test
    void addCounters() {
        var counters = Counters.create();

        counters.add(CounterType.PLUS_ONE_PLUS_ONE, 3);

        assertThat(counters).isNotEmpty().hasPlusOnePlusOneCounters(3);
    }

    @Test
    void addMultipleCounterTypes() {
        var counters = Counters.create();

        counters.add(CounterType.PLUS_ONE_PLUS_ONE, 2);
        counters.add(CounterType.LOYALTY, 4);

        assertThat(counters).hasPlusOnePlusOneCounters(2).hasLoyaltyCounters(4);
    }

    @Test
    void addToExistingCounters() {
        var counters = Counters.create();

        counters.add(CounterType.PLUS_ONE_PLUS_ONE, 2);
        counters.add(CounterType.PLUS_ONE_PLUS_ONE, 3);

        assertThat(counters).hasPlusOnePlusOneCounters(5);
    }

    @Test
    void removeCounters() {
        var counters = Counters.create();

        counters.add(CounterType.LOYALTY, 5);
        counters.remove(CounterType.LOYALTY, 2);

        assertThat(counters).hasLoyaltyCounters(3);
    }

    @Test
    void removeAllCounters() {
        var counters = Counters.create();

        counters.add(CounterType.MINUS_ONE_MINUS_ONE, 3);
        counters.remove(CounterType.MINUS_ONE_MINUS_ONE, 3);

        assertThat(counters).hasMinusOneMinusOneCounters(0);
    }
}
