package be.imgn.mtg.engine.ability.internal.parser.assertions;

import org.assertj.core.api.AbstractAssert;

import be.imgn.mtg.engine.effect.UntapEffect;

/// Assertion class for UntapEffect.
public class UntapEffectAssert extends AbstractAssert<UntapEffectAssert, UntapEffect> {

    protected UntapEffectAssert(UntapEffect actual) {
        super(actual, UntapEffectAssert.class);
    }

    public static UntapEffectAssert assertThat(UntapEffect actual) {
        return new UntapEffectAssert(actual);
    }

    /// Returns an assertion on the subject.
    public SubjectAssert subject() {
        isNotNull();
        return new SubjectAssert(actual.subject());
    }
}
