package be.imgn.mtg.engine.mana;

import org.assertj.core.api.AbstractAssert;

/// Assertion class for AddExactManaEffect.
public class AddExactManaEffectAssert extends AbstractAssert<AddExactManaEffectAssert, AddExactManaEffect> {

    public AddExactManaEffectAssert(AddExactManaEffect actual) {
        super(actual, AddExactManaEffectAssert.class);
    }

    public static AddExactManaEffectAssert assertThat(AddExactManaEffect actual) {
        return new AddExactManaEffectAssert(actual);
    }

    /// Verifies that the mana string equals the expected value.
    public AddExactManaEffectAssert hasMana(String expected) {
        isNotNull();
        if (!actual.mana().equals(expected)) {
            failWithMessage("Expected mana to be <%s> but was <%s>", expected, actual.mana());
        }
        return this;
    }
}
