package be.imgn.mtg.engine.ability.internal.parser.assertions;

import org.assertj.core.api.AbstractAssert;

import be.imgn.mtg.engine.ability.internal.parser.effect.DrawEffect;
import be.imgn.mtg.engine.ability.internal.parser.selector.Amount;
import be.imgn.mtg.engine.selector.PlayerReference;

/// Assertion class for DrawEffect.
public class DrawEffectAssert extends AbstractAssert<DrawEffectAssert, DrawEffect> {

    protected DrawEffectAssert(DrawEffect actual) {
        super(actual, DrawEffectAssert.class);
    }

    public static DrawEffectAssert assertThat(DrawEffect actual) {
        return new DrawEffectAssert(actual);
    }

    /// Verifies that the amount equals the expected value.
    public DrawEffectAssert hasAmount(int expected) {
        isNotNull();
        var expectedAmount = new Amount.Exact(expected);
        if (!actual.amount().equals(expectedAmount)) {
            failWithMessage("Expected amount to be <%s> but was <%s>", expectedAmount, actual.amount());
        }
        return this;
    }

    /// Verifies that the amount equals the expected Amount.
    public DrawEffectAssert hasAmount(Amount expected) {
        isNotNull();
        if (!actual.amount().equals(expected)) {
            failWithMessage("Expected amount to be <%s> but was <%s>", expected, actual.amount());
        }
        return this;
    }

    /// Verifies that the player is implicitly "you".
    public DrawEffectAssert hasImplicitPlayer() {
        isNotNull();
        if (actual.player().isPresent()) {
            failWithMessage(
                    "Expected player to be implicit (you) but was <%s>",
                    actual.player().get());
        }
        return this;
    }

    /// Verifies that the player reference equals the expected value.
    public DrawEffectAssert hasPlayer(PlayerReference expected) {
        isNotNull();
        if (actual.player().isEmpty()) {
            failWithMessage("Expected player to be <%s> but was implicit (you)", expected);
            return this;
        }
        if (!actual.player().get().equals(expected)) {
            failWithMessage(
                    "Expected player to be <%s> but was <%s>",
                    expected, actual.player().get());
        }
        return this;
    }
}
