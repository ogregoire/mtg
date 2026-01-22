package be.imgn.mtg.engine.mana;

import org.assertj.core.api.AbstractAssert;

/// Assertion class for AddManaEffect.Variable.
public class AddVariableManaEffectAssert extends AbstractAssert<AddVariableManaEffectAssert, AddManaEffect.Variable> {

    public AddVariableManaEffectAssert(AddManaEffect.Variable actual) {
        super(actual, AddVariableManaEffectAssert.class);
    }

    public static AddVariableManaEffectAssert assertThat(AddManaEffect.Variable actual) {
        return new AddVariableManaEffectAssert(actual);
    }

    /// Verifies that the mana type equals the expected value.
    public AddVariableManaEffectAssert hasMana(ManaType expected) {
        isNotNull();
        if (!actual.mana().equals(expected)) {
            failWithMessage("Expected mana to be <%s> but was <%s>", expected, actual.mana());
        }
        return this;
    }
}
