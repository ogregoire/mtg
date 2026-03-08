package be.imgn.mtg.engine.ability.internal.parser.assertions;

import org.assertj.core.api.AbstractAssert;

import be.imgn.mtg.engine.effect.DestroyEffect;

/// Assertion class for DestroyEffect.
public class DestroyEffectAssert extends AbstractAssert<DestroyEffectAssert, DestroyEffect> {

    protected DestroyEffectAssert(DestroyEffect actual) {
        super(actual, DestroyEffectAssert.class);
    }

    public static DestroyEffectAssert assertThat(DestroyEffect actual) {
        return new DestroyEffectAssert(actual);
    }

    /// Returns an assertion on the subject.
    public SubjectAssert subject() {
        isNotNull();
        return new SubjectAssert(actual.subject());
    }

    /// Verifies that the destroyed permanent can be regenerated.
    public DestroyEffectAssert canBeRegenerated() {
        isNotNull();
        if (!actual.canBeRegenerated()) {
            failWithMessage("Expected canBeRegenerated to be true but was false");
        }
        return this;
    }

    /// Verifies that the destroyed permanent cannot be regenerated.
    public DestroyEffectAssert cannotBeRegenerated() {
        isNotNull();
        if (actual.canBeRegenerated()) {
            failWithMessage("Expected canBeRegenerated to be false but was true");
        }
        return this;
    }
}
