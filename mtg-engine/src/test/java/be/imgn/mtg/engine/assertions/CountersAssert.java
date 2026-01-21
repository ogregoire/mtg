package be.imgn.mtg.engine.assertions;

import org.assertj.core.api.AbstractObjectAssert;

import be.imgn.mtg.engine.characteristics.CounterType;
import be.imgn.mtg.engine.characteristics.Counters;
import be.imgn.mtg.engine.characteristics.StandardCounterType;

/// Assertion class for Counters.
public class CountersAssert extends AbstractObjectAssert<CountersAssert, Counters> {

    protected CountersAssert(Counters actual) {
        super(actual, CountersAssert.class);
    }

    public static CountersAssert assertThat(Counters actual) {
        return new CountersAssert(actual);
    }

    public CountersAssert isEmpty() {
        isNotNull();
        if (!actual.isEmpty()) {
            failWithMessage("Expected counters to be empty");
        }
        return this;
    }

    public CountersAssert isNotEmpty() {
        isNotNull();
        if (actual.isEmpty()) {
            failWithMessage("Expected counters not to be empty");
        }
        return this;
    }

    public CountersAssert hasCount(CounterType counterType, int expected) {
        isNotNull();
        int count = actual.count(counterType);
        if (count != expected) {
            failWithMessage("Expected <%d> %s counters but had <%d>", expected, counterType.text(), count);
        }
        return this;
    }

    public CountersAssert hasNoCounters(CounterType counterType) {
        return hasCount(counterType, 0);
    }

    public CountersAssert hasLoyaltyCounters(int expected) {
        return hasCount(StandardCounterType.LOYALTY, expected);
    }

    public CountersAssert hasPlusOnePlusOneCounters(int expected) {
        return hasCount(StandardCounterType.PLUS_ONE_PLUS_ONE, expected);
    }

    public CountersAssert hasMinusOneMinusOneCounters(int expected) {
        return hasCount(StandardCounterType.MINUS_ONE_MINUS_ONE, expected);
    }
}
