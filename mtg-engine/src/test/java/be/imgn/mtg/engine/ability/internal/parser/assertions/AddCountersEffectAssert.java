package be.imgn.mtg.engine.ability.internal.parser.assertions;

import org.assertj.core.api.AbstractAssert;

import be.imgn.mtg.engine.ability.internal.parser.selector.Amount;
import be.imgn.mtg.engine.characteristics.CounterType;
import be.imgn.mtg.engine.characteristics.StandardCounterType;
import be.imgn.mtg.engine.effect.AddCountersEffect;

/// Assertion class for AddCountersEffect.
public class AddCountersEffectAssert extends AbstractAssert<AddCountersEffectAssert, AddCountersEffect> {

    protected AddCountersEffectAssert(AddCountersEffect actual) {
        super(actual, AddCountersEffectAssert.class);
    }

    public static AddCountersEffectAssert assertThat(AddCountersEffect actual) {
        return new AddCountersEffectAssert(actual);
    }

    /// Verifies that the amount equals the expected value.
    public AddCountersEffectAssert hasAmount(int expected) {
        isNotNull();
        var expectedAmount = new Amount.Exact(expected);
        if (!actual.amount().equals(expectedAmount)) {
            failWithMessage("Expected amount to be <%s> but was <%s>", expectedAmount, actual.amount());
        }
        return this;
    }

    /// Verifies that the amount equals the expected Amount.
    public AddCountersEffectAssert hasAmount(Amount expected) {
        isNotNull();
        if (!actual.amount().equals(expected)) {
            failWithMessage("Expected amount to be <%s> but was <%s>", expected, actual.amount());
        }
        return this;
    }

    /// Verifies that the counter type equals the expected value.
    public AddCountersEffectAssert hasCounterType(CounterType expected) {
        isNotNull();
        if (!actual.counterType().equals(expected)) {
            failWithMessage("Expected counter type to be <%s> but was <%s>", expected, actual.counterType());
        }
        return this;
    }

    /// Verifies that the counter type is +1/+1.
    public AddCountersEffectAssert isPlusOnePlusOneCounter() {
        return hasCounterType(StandardCounterType.PLUS_ONE_PLUS_ONE);
    }

    /// Verifies that the counter type is -1/-1.
    public AddCountersEffectAssert isMinusOneMinusOneCounter() {
        return hasCounterType(StandardCounterType.MINUS_ONE_MINUS_ONE);
    }

    /// Returns an assertion on the subject.
    public SubjectAssert subject() {
        isNotNull();
        return new SubjectAssert(actual.subject());
    }
}
