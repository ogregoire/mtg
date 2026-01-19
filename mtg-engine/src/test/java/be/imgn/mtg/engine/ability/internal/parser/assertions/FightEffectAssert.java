package be.imgn.mtg.engine.ability.internal.parser.assertions;

import org.assertj.core.api.AbstractAssert;

import be.imgn.mtg.engine.ability.internal.parser.effect.FightEffect;

/// Assertion class for FightEffect.
public class FightEffectAssert extends AbstractAssert<FightEffectAssert, FightEffect> {

    protected FightEffectAssert(FightEffect actual) {
        super(actual, FightEffectAssert.class);
    }

    public static FightEffectAssert assertThat(FightEffect actual) {
        return new FightEffectAssert(actual);
    }

    /// Returns an assertion on the first creature.
    public SubjectAssert firstCreature() {
        isNotNull();
        return new SubjectAssert(actual.firstCreature());
    }

    /// Returns an assertion on the second creature.
    public SubjectAssert secondCreature() {
        isNotNull();
        return new SubjectAssert(actual.secondCreature());
    }
}
