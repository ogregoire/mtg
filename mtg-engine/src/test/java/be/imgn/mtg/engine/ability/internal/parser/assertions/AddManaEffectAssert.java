package be.imgn.mtg.engine.ability.internal.parser.assertions;

import org.assertj.core.api.AbstractAssert;

import be.imgn.mtg.engine.ability.internal.parser.effect.AddManaEffect;

/// Assertion class for AddManaEffect.
public class AddManaEffectAssert extends AbstractAssert<AddManaEffectAssert, AddManaEffect> {

    protected AddManaEffectAssert(AddManaEffect actual) {
        super(actual, AddManaEffectAssert.class);
    }

    public static AddManaEffectAssert assertThat(AddManaEffect actual) {
        return new AddManaEffectAssert(actual);
    }

    /// Verifies that the mana string equals the expected value.
    public AddManaEffectAssert hasMana(String expected) {
        isNotNull();
        if (!actual.mana().equals(expected)) {
            failWithMessage("Expected mana to be <%s> but was <%s>", expected, actual.mana());
        }
        return this;
    }
}
