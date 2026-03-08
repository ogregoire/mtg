package be.imgn.mtg.engine.ability.internal.parser.assertions;

import org.assertj.core.api.AbstractAssert;

import be.imgn.mtg.engine.effect.CounterSpellEffect;

/// Assertion class for CounterSpellEffect.
public class CounterSpellEffectAssert extends AbstractAssert<CounterSpellEffectAssert, CounterSpellEffect> {

    protected CounterSpellEffectAssert(CounterSpellEffect actual) {
        super(actual, CounterSpellEffectAssert.class);
    }

    public static CounterSpellEffectAssert assertThat(CounterSpellEffect actual) {
        return new CounterSpellEffectAssert(actual);
    }

    /// Returns an assertion on the subject.
    public SubjectAssert subject() {
        isNotNull();
        return new SubjectAssert(actual.subject());
    }
}
