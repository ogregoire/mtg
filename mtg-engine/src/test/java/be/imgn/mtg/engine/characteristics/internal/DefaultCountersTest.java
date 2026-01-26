package be.imgn.mtg.engine.characteristics.internal;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.assertions.MTGAssertions;
import be.imgn.mtg.engine.characteristics.StandardCounterType;

class DefaultCountersTest {

    private static final StandardCounterType LOYALTY = StandardCounterType.LOYALTY;
    private static final StandardCounterType PLUS_ONE_PLUS_ONE = StandardCounterType.PLUS_ONE_PLUS_ONE;
    private static final StandardCounterType MINUS_ONE_MINUS_ONE = StandardCounterType.MINUS_ONE_MINUS_ONE;

    @Test
    void createCountersIsEmpty() {
        var counters = DefaultCounters.create();

        MTGAssertions.assertThat(counters).isEmpty();
    }

    @Test
    void addCountersMakesNotEmpty() {
        var counters = DefaultCounters.create();

        counters.add(LOYALTY, 3);

        MTGAssertions.assertThat(counters).isNotEmpty();
    }

    @Test
    void countReturnsZeroForMissingType() {
        var counters = DefaultCounters.create();

        assertThat(counters.count(LOYALTY)).isEqualTo(0);
    }

    @Test
    void countReturnsCorrectValue() {
        var counters = DefaultCounters.create();
        counters.add(PLUS_ONE_PLUS_ONE, 5);

        assertThat(counters.count(PLUS_ONE_PLUS_ONE)).isEqualTo(5);
    }

    @Test
    void removeCountersUpdatesCount() {
        var counters = DefaultCounters.create();
        counters.add(LOYALTY, 7);
        counters.remove(LOYALTY, 3);

        assertThat(counters.count(LOYALTY)).isEqualTo(4);
    }

    @Test
    void removeAllCountersMakesEmpty() {
        var counters = DefaultCounters.create();
        counters.add(MINUS_ONE_MINUS_ONE, 2);
        counters.remove(MINUS_ONE_MINUS_ONE, 2);

        MTGAssertions.assertThat(counters).isEmpty();
    }

    @Test
    void multipleCounterTypes() {
        var counters = DefaultCounters.create();
        counters.add(LOYALTY, 4);
        counters.add(PLUS_ONE_PLUS_ONE, 2);

        MTGAssertions.assertThat(counters).isNotEmpty();
        assertThat(counters.count(LOYALTY)).isEqualTo(4);
        assertThat(counters.count(PLUS_ONE_PLUS_ONE)).isEqualTo(2);
    }
}
