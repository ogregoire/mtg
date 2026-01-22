package be.imgn.mtg.engine.mana;

import java.util.List;

import org.assertj.core.api.AbstractAssert;

/// Assertion class for AddManaEffect.Exact.
public class AddExactManaEffectAssert extends AbstractAssert<AddExactManaEffectAssert, AddManaEffect.Exact> {

    public AddExactManaEffectAssert(AddManaEffect.Exact actual) {
        super(actual, AddExactManaEffectAssert.class);
    }

    public static AddExactManaEffectAssert assertThat(AddManaEffect.Exact actual) {
        return new AddExactManaEffectAssert(actual);
    }

    /// Verifies that the mana list equals the expected value.
    public AddExactManaEffectAssert hasMana(List<ManaType> expected) {
        isNotNull();
        if (!actual.mana().equals(expected)) {
            failWithMessage("Expected mana to be <%s> but was <%s>", expected, actual.mana());
        }
        return this;
    }

    /// Verifies that the mana list equals the expected mana types.
    public AddExactManaEffectAssert hasMana(ManaType... expected) {
        return hasMana(List.of(expected));
    }
}
