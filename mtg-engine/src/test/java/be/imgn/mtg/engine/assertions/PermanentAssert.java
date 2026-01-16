package be.imgn.mtg.engine.assertions;

import be.imgn.mtg.engine.characteristics.CounterType;
import be.imgn.mtg.engine.object.Permanent;

/// Assertion class for Permanent.
public class PermanentAssert extends AbstractGameObjectAssert<PermanentAssert, Permanent> {

    protected PermanentAssert(Permanent actual) {
        super(actual, PermanentAssert.class);
    }

    public static PermanentAssert assertThat(Permanent actual) {
        return new PermanentAssert(actual);
    }

    // Tapped state

    public PermanentAssert isTapped() {
        isNotNull();
        if (!actual.isTapped()) {
            failWithMessage("Expected permanent to be tapped but was untapped");
        }
        return this;
    }

    public PermanentAssert isUntapped() {
        isNotNull();
        if (!actual.isUntapped()) {
            failWithMessage("Expected permanent to be untapped but was tapped");
        }
        return this;
    }

    // Flipped state

    public PermanentAssert isFlipped() {
        isNotNull();
        if (!actual.isFlipped()) {
            failWithMessage("Expected permanent to be flipped but was unflipped");
        }
        return this;
    }

    public PermanentAssert isUnflipped() {
        isNotNull();
        if (!actual.isUnflipped()) {
            failWithMessage("Expected permanent to be unflipped but was flipped");
        }
        return this;
    }

    // Face up/down state

    public PermanentAssert isFaceUp() {
        isNotNull();
        if (!actual.isFaceUp()) {
            failWithMessage("Expected permanent to be face up but was face down");
        }
        return this;
    }

    public PermanentAssert isFaceDown() {
        isNotNull();
        if (!actual.isFaceDown()) {
            failWithMessage("Expected permanent to be face down but was face up");
        }
        return this;
    }

    // Phased state

    public PermanentAssert isPhasedIn() {
        isNotNull();
        if (!actual.isPhasedIn()) {
            failWithMessage("Expected permanent to be phased in but was phased out");
        }
        return this;
    }

    public PermanentAssert isPhasedOut() {
        isNotNull();
        if (!actual.isPhasedOut()) {
            failWithMessage("Expected permanent to be phased out but was phased in");
        }
        return this;
    }

    // Counter assertions

    public PermanentAssert hasCounters(CounterType counterType, int expected) {
        isNotNull();
        int count = actual.counters().count(counterType);
        if (count != expected) {
            failWithMessage("Expected <%d> %s counters but had <%d>", expected, counterType.text(), count);
        }
        return this;
    }

    public PermanentAssert hasNoCounters(CounterType counterType) {
        return hasCounters(counterType, 0);
    }

    public PermanentAssert hasNoCounters() {
        isNotNull();
        if (!actual.counters().isEmpty()) {
            failWithMessage("Expected no counters but had some");
        }
        return this;
    }
}
