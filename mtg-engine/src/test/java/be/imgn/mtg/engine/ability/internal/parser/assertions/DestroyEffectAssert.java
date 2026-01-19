package be.imgn.mtg.engine.ability.internal.parser.assertions;

import org.assertj.core.api.AbstractAssert;

import be.imgn.mtg.engine.ability.internal.parser.effect.DestroyEffect;

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
}
