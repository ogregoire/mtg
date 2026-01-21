package be.imgn.mtg.engine.characteristics;

import static be.imgn.mtg.engine.assertions.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CountersTest {

    // Import standard counter types for brevity
    private static final StandardCounterType LOYALTY = StandardCounterType.LOYALTY;
    private static final StandardCounterType PLUS_ONE_PLUS_ONE = StandardCounterType.PLUS_ONE_PLUS_ONE;
    private static final StandardCounterType MINUS_ONE_MINUS_ONE = StandardCounterType.MINUS_ONE_MINUS_ONE;

    @Test
    void newCountersAreEmpty() {
        var counters = Counters.create();

        assertThat(counters).isEmpty().hasCount(LOYALTY, 0).hasCount(PLUS_ONE_PLUS_ONE, 0);
    }

    @Test
    void addCounters() {
        var counters = Counters.create();

        counters.add(PLUS_ONE_PLUS_ONE, 3);

        assertThat(counters).isNotEmpty().hasPlusOnePlusOneCounters(3);
    }

    @Test
    void addMultipleCounterTypes() {
        var counters = Counters.create();

        counters.add(PLUS_ONE_PLUS_ONE, 2);
        counters.add(LOYALTY, 4);

        assertThat(counters).hasPlusOnePlusOneCounters(2).hasLoyaltyCounters(4);
    }

    @Test
    void addToExistingCounters() {
        var counters = Counters.create();

        counters.add(PLUS_ONE_PLUS_ONE, 2);
        counters.add(PLUS_ONE_PLUS_ONE, 3);

        assertThat(counters).hasPlusOnePlusOneCounters(5);
    }

    @Test
    void removeCounters() {
        var counters = Counters.create();

        counters.add(LOYALTY, 5);
        counters.remove(LOYALTY, 2);

        assertThat(counters).hasLoyaltyCounters(3);
    }

    @Test
    void removeAllCounters() {
        var counters = Counters.create();

        counters.add(MINUS_ONE_MINUS_ONE, 3);
        counters.remove(MINUS_ONE_MINUS_ONE, 3);

        assertThat(counters).hasMinusOneMinusOneCounters(0);
    }
}
