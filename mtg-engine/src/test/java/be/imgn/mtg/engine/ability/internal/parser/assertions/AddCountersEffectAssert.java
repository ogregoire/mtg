package be.imgn.mtg.engine.ability.internal.parser.assertions;

import org.assertj.core.api.AbstractAssert;

import be.imgn.mtg.engine.ability.internal.parser.effect.AddCountersEffect;
import be.imgn.mtg.engine.ability.internal.parser.selector.Amount;

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
    public AddCountersEffectAssert hasCounterType(String expected) {
        isNotNull();
        if (!actual.counterType().equals(expected)) {
            failWithMessage("Expected counter type to be <%s> but was <%s>", expected, actual.counterType());
        }
        return this;
    }

    /// Verifies that the counter type is +1/+1.
    public AddCountersEffectAssert isPlusOnePlusOneCounter() {
        return hasCounterType("+1/+1");
    }

    /// Verifies that the counter type is -1/-1.
    public AddCountersEffectAssert isMinusOneMinusOneCounter() {
        return hasCounterType("-1/-1");
    }

    /// Returns an assertion on the subject.
    public SubjectAssert subject() {
        isNotNull();
        return new SubjectAssert(actual.subject());
    }
}
