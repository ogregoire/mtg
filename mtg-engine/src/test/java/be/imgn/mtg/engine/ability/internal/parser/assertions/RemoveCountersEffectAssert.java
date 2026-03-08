package be.imgn.mtg.engine.ability.internal.parser.assertions;

import org.assertj.core.api.AbstractAssert;

import be.imgn.mtg.engine.ability.internal.parser.selector.Amount;
import be.imgn.mtg.engine.characteristics.CounterType;
import be.imgn.mtg.engine.effect.RemoveCountersEffect;

/// Assertion class for RemoveCountersEffect.
public class RemoveCountersEffectAssert extends AbstractAssert<RemoveCountersEffectAssert, RemoveCountersEffect> {

    protected RemoveCountersEffectAssert(RemoveCountersEffect actual) {
        super(actual, RemoveCountersEffectAssert.class);
    }

    public static RemoveCountersEffectAssert assertThat(RemoveCountersEffect actual) {
        return new RemoveCountersEffectAssert(actual);
    }

    /// Verifies that the amount equals the expected value.
    public RemoveCountersEffectAssert hasAmount(int expected) {
        isNotNull();
        var expectedAmount = new Amount.Exact(expected);
        if (!actual.amount().equals(expectedAmount)) {
            failWithMessage("Expected amount to be <%s> but was <%s>", expectedAmount, actual.amount());
        }
        return this;
    }

    /// Verifies that the amount equals the expected Amount.
    public RemoveCountersEffectAssert hasAmount(Amount expected) {
        isNotNull();
        if (!actual.amount().equals(expected)) {
            failWithMessage("Expected amount to be <%s> but was <%s>", expected, actual.amount());
        }
        return this;
    }

    /// Verifies that the counter type equals the expected value.
    public RemoveCountersEffectAssert hasCounterType(CounterType expected) {
        isNotNull();
        if (!actual.counterType().equals(expected)) {
            failWithMessage("Expected counter type to be <%s> but was <%s>", expected, actual.counterType());
        }
        return this;
    }

    /// Returns an assertion on the subject.
    public SubjectAssert subject() {
        isNotNull();
        return new SubjectAssert(actual.subject());
    }
}
