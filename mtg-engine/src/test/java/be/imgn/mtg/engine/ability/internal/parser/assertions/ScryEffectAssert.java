package be.imgn.mtg.engine.ability.internal.parser.assertions;

import org.assertj.core.api.AbstractAssert;

import be.imgn.mtg.engine.ability.internal.parser.selector.Amount;
import be.imgn.mtg.engine.effect.ScryEffect;

/// Assertion class for ScryEffect.
public class ScryEffectAssert extends AbstractAssert<ScryEffectAssert, ScryEffect> {

    protected ScryEffectAssert(ScryEffect actual) {
        super(actual, ScryEffectAssert.class);
    }

    public static ScryEffectAssert assertThat(ScryEffect actual) {
        return new ScryEffectAssert(actual);
    }

    /// Verifies that the amount equals the expected value.
    public ScryEffectAssert hasAmount(int expected) {
        isNotNull();
        var expectedAmount = new Amount.Exact(expected);
        if (!actual.amount().equals(expectedAmount)) {
            failWithMessage("Expected amount to be <%s> but was <%s>", expectedAmount, actual.amount());
        }
        return this;
    }

    /// Verifies that the amount equals the expected Amount.
    public ScryEffectAssert hasAmount(Amount expected) {
        isNotNull();
        if (!actual.amount().equals(expected)) {
            failWithMessage("Expected amount to be <%s> but was <%s>", expected, actual.amount());
        }
        return this;
    }
}
