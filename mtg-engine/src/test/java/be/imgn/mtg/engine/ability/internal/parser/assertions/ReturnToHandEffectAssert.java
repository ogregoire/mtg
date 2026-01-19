package be.imgn.mtg.engine.ability.internal.parser.assertions;

import org.assertj.core.api.AbstractAssert;

import be.imgn.mtg.engine.ability.internal.parser.effect.ReturnToHandEffect;

/// Assertion class for ReturnToHandEffect.
public class ReturnToHandEffectAssert extends AbstractAssert<ReturnToHandEffectAssert, ReturnToHandEffect> {

    protected ReturnToHandEffectAssert(ReturnToHandEffect actual) {
        super(actual, ReturnToHandEffectAssert.class);
    }

    public static ReturnToHandEffectAssert assertThat(ReturnToHandEffect actual) {
        return new ReturnToHandEffectAssert(actual);
    }

    /// Returns an assertion on the subject.
    public SubjectAssert subject() {
        isNotNull();
        return new SubjectAssert(actual.subject());
    }
}
