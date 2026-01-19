package be.imgn.mtg.engine.ability.internal.parser.assertions;

import org.assertj.core.api.AbstractAssert;

import be.imgn.mtg.engine.ability.internal.parser.effect.CreateTokenEffect;
import be.imgn.mtg.engine.ability.internal.parser.selector.Amount;

/// Assertion class for CreateTokenEffect.
public class CreateTokenEffectAssert extends AbstractAssert<CreateTokenEffectAssert, CreateTokenEffect> {

    protected CreateTokenEffectAssert(CreateTokenEffect actual) {
        super(actual, CreateTokenEffectAssert.class);
    }

    public static CreateTokenEffectAssert assertThat(CreateTokenEffect actual) {
        return new CreateTokenEffectAssert(actual);
    }

    /// Verifies that the effect is a token and returns a specialized assert.
    public TokenAssert isToken() {
        isNotNull();
        if (!(actual instanceof CreateTokenEffect.Token)) {
            failWithMessage("Expected a Token but was <%s>", actual.getClass().getSimpleName());
        }
        return new TokenAssert((CreateTokenEffect.Token) actual);
    }

    /// Verifies that the effect is a predefined token and returns a specialized assert.
    public PredefinedTokenAssert isPredefinedToken() {
        isNotNull();
        if (!(actual instanceof CreateTokenEffect.Predefined)) {
            failWithMessage(
                    "Expected a Predefined token but was <%s>",
                    actual.getClass().getSimpleName());
        }
        return new PredefinedTokenAssert((CreateTokenEffect.Predefined) actual);
    }

    /// Verifies that the effect is a token by card name and returns a specialized assert.
    public ByCardNameTokenAssert isByCardName() {
        isNotNull();
        if (!(actual instanceof CreateTokenEffect.ByCardName)) {
            failWithMessage(
                    "Expected a ByCardName token but was <%s>",
                    actual.getClass().getSimpleName());
        }
        return new ByCardNameTokenAssert((CreateTokenEffect.ByCardName) actual);
    }

    /// Verifies that the token amount equals the expected value.
    public CreateTokenEffectAssert hasAmount(int expected) {
        isNotNull();
        var expectedAmount = new Amount.Exact(expected);
        if (!actual.amount().equals(expectedAmount)) {
            failWithMessage("Expected amount to be <%s> but was <%s>", expectedAmount, actual.amount());
        }
        return this;
    }

    /// Verifies that the token amount equals the expected Amount.
    public CreateTokenEffectAssert hasAmount(Amount expected) {
        isNotNull();
        if (!actual.amount().equals(expected)) {
            failWithMessage("Expected amount to be <%s> but was <%s>", expected, actual.amount());
        }
        return this;
    }
}
