package be.imgn.mtg.engine.ability.internal.parser.assertions;

import org.assertj.core.api.AbstractAssert;

import be.imgn.mtg.engine.ability.internal.parser.selector.Amount;
import be.imgn.mtg.engine.effect.CreateTokenEffect;

/// Assertion class for CreateTokenEffect.ByCardName.
public class ByCardNameTokenAssert extends AbstractAssert<ByCardNameTokenAssert, CreateTokenEffect.ByCardName> {

    protected ByCardNameTokenAssert(CreateTokenEffect.ByCardName actual) {
        super(actual, ByCardNameTokenAssert.class);
    }

    public static ByCardNameTokenAssert assertThat(CreateTokenEffect.ByCardName actual) {
        return new ByCardNameTokenAssert(actual);
    }

    /// Verifies that the token amount equals the expected value.
    public ByCardNameTokenAssert hasAmount(int expected) {
        isNotNull();
        var expectedAmount = new Amount.Exact(expected);
        if (!actual.amount().equals(expectedAmount)) {
            failWithMessage("Expected amount to be <%s> but was <%s>", expectedAmount, actual.amount());
        }
        return this;
    }

    /// Verifies that the token amount is variable (X).
    public ByCardNameTokenAssert hasVariableAmount() {
        isNotNull();
        if (actual.amount() != Amount.X) {
            failWithMessage("Expected variable amount (X) but was <%s>", actual.amount());
        }
        return this;
    }

    /// Verifies that the card name equals the expected value.
    public ByCardNameTokenAssert hasCardName(String expected) {
        isNotNull();
        if (!actual.cardName().equals(expected)) {
            failWithMessage("Expected card name to be <%s> but was <%s>", expected, actual.cardName());
        }
        return this;
    }
}
