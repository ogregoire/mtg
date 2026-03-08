package be.imgn.mtg.engine.ability.internal.parser.assertions;

import org.assertj.core.api.AbstractAssert;

import be.imgn.mtg.engine.effect.SacrificeEffect;

/// Assertion class for SacrificeEffect.
public class SacrificeEffectAssert extends AbstractAssert<SacrificeEffectAssert, SacrificeEffect> {

    protected SacrificeEffectAssert(SacrificeEffect actual) {
        super(actual, SacrificeEffectAssert.class);
    }

    public static SacrificeEffectAssert assertThat(SacrificeEffect actual) {
        return new SacrificeEffectAssert(actual);
    }

    /// Returns an assertion on the subject.
    public SubjectAssert subject() {
        isNotNull();
        return new SubjectAssert(actual.subject());
    }
}
