package be.imgn.mtg.engine.ability.internal.parser.assertions;

import org.assertj.core.api.AbstractAssert;

import be.imgn.mtg.engine.effect.TapEffect;

/// Assertion class for TapEffect.
public class TapEffectAssert extends AbstractAssert<TapEffectAssert, TapEffect> {

    protected TapEffectAssert(TapEffect actual) {
        super(actual, TapEffectAssert.class);
    }

    public static TapEffectAssert assertThat(TapEffect actual) {
        return new TapEffectAssert(actual);
    }

    /// Returns an assertion on the subject.
    public SubjectAssert subject() {
        isNotNull();
        return new SubjectAssert(actual.subject());
    }
}
