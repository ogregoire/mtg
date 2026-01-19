package be.imgn.mtg.engine.ability.internal.parser.assertions;

import org.assertj.core.api.AbstractAssert;

import be.imgn.mtg.engine.ability.internal.parser.effect.LoseLifeEffect;
import be.imgn.mtg.engine.ability.internal.parser.reference.PlayerReference;
import be.imgn.mtg.engine.ability.internal.parser.selector.Amount;

/// Assertion class for LoseLifeEffect.
public class LoseLifeEffectAssert extends AbstractAssert<LoseLifeEffectAssert, LoseLifeEffect> {

    protected LoseLifeEffectAssert(LoseLifeEffect actual) {
        super(actual, LoseLifeEffectAssert.class);
    }

    public static LoseLifeEffectAssert assertThat(LoseLifeEffect actual) {
        return new LoseLifeEffectAssert(actual);
    }

    /// Verifies that the amount equals the expected value.
    public LoseLifeEffectAssert hasAmount(int expected) {
        isNotNull();
        var expectedAmount = new Amount.Exact(expected);
        if (!actual.amount().equals(expectedAmount)) {
            failWithMessage("Expected amount to be <%s> but was <%s>", expectedAmount, actual.amount());
        }
        return this;
    }

    /// Verifies that the amount equals the expected Amount.
    public LoseLifeEffectAssert hasAmount(Amount expected) {
        isNotNull();
        if (!actual.amount().equals(expected)) {
            failWithMessage("Expected amount to be <%s> but was <%s>", expected, actual.amount());
        }
        return this;
    }

    /// Verifies that the player is implicitly "you".
    public LoseLifeEffectAssert hasImplicitPlayer() {
        isNotNull();
        if (actual.player().isPresent()) {
            failWithMessage(
                    "Expected player to be implicit (you) but was <%s>",
                    actual.player().get());
        }
        return this;
    }

    /// Verifies that the player reference equals the expected value.
    public LoseLifeEffectAssert hasPlayer(PlayerReference expected) {
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
