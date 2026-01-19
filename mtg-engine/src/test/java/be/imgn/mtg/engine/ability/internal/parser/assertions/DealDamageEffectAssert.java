package be.imgn.mtg.engine.ability.internal.parser.assertions;

import org.assertj.core.api.AbstractAssert;

import be.imgn.mtg.engine.ability.internal.parser.effect.DealDamageEffect;
import be.imgn.mtg.engine.ability.internal.parser.selector.Amount;

/// Assertion class for DealDamageEffect.
public class DealDamageEffectAssert extends AbstractAssert<DealDamageEffectAssert, DealDamageEffect> {

    protected DealDamageEffectAssert(DealDamageEffect actual) {
        super(actual, DealDamageEffectAssert.class);
    }

    public static DealDamageEffectAssert assertThat(DealDamageEffect actual) {
        return new DealDamageEffectAssert(actual);
    }

    /// Verifies that the amount equals the expected value.
    public DealDamageEffectAssert hasAmount(int expected) {
        isNotNull();
        var expectedAmount = new Amount.Exact(expected);
        if (!actual.amount().equals(expectedAmount)) {
            failWithMessage("Expected amount to be <%s> but was <%s>", expectedAmount, actual.amount());
        }
        return this;
    }

    /// Verifies that the amount equals the expected Amount.
    public DealDamageEffectAssert hasAmount(Amount expected) {
        isNotNull();
        if (!actual.amount().equals(expected)) {
            failWithMessage("Expected amount to be <%s> but was <%s>", expected, actual.amount());
        }
        return this;
    }

    /// Returns an assertion on the target.
    public SubjectAssert target() {
        isNotNull();
        return new SubjectAssert(actual.target());
    }
}
