package be.imgn.mtg.engine.ability.internal.parser.assertions;

import org.assertj.core.api.AbstractAssert;

import be.imgn.mtg.engine.effect.ExileEffect;

/// Assertion class for ExileEffect.
public class ExileEffectAssert extends AbstractAssert<ExileEffectAssert, ExileEffect> {

    protected ExileEffectAssert(ExileEffect actual) {
        super(actual, ExileEffectAssert.class);
    }

    public static ExileEffectAssert assertThat(ExileEffect actual) {
        return new ExileEffectAssert(actual);
    }

    /// Returns an assertion on the subject.
    public SubjectAssert subject() {
        isNotNull();
        return new SubjectAssert(actual.subject());
    }
}
