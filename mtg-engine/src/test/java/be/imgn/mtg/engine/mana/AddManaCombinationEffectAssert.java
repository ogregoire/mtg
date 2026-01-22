package be.imgn.mtg.engine.mana;

import java.util.Set;

import org.assertj.core.api.AbstractAssert;

/// Assertion class for AddManaOfAnyCombinationEffect.
public class AddManaCombinationEffectAssert
        extends AbstractAssert<AddManaCombinationEffectAssert, AddManaCombinationEffect> {

    public AddManaCombinationEffectAssert(AddManaCombinationEffect actual) {
        super(actual, AddManaCombinationEffectAssert.class);
    }

    public static AddManaCombinationEffectAssert assertThat(AddManaCombinationEffect actual) {
        return new AddManaCombinationEffectAssert(actual);
    }

    /// Verifies that the amount equals the expected value.
    public AddManaCombinationEffectAssert hasAmount(int expected) {
        isNotNull();
        if (actual.amount() != expected) {
            failWithMessage("Expected amount to be <%d> but was <%d>", expected, actual.amount());
        }
        return this;
    }

    /// Verifies that the allowed types equal the expected set.
    public AddManaCombinationEffectAssert hasAllowedTypes(Set<ManaType> expected) {
        isNotNull();
        if (!actual.allowedTypes().equals(expected)) {
            failWithMessage("Expected allowedTypes to be <%s> but was <%s>", expected, actual.allowedTypes());
        }
        return this;
    }

    /// Verifies that the allowed types contain all specified types.
    public AddManaCombinationEffectAssert hasAllowedTypesContaining(ManaType... expected) {
        isNotNull();
        for (var type : expected) {
            if (!actual.allowedTypes().contains(type)) {
                failWithMessage("Expected allowedTypes to contain <%s> but was <%s>", type, actual.allowedTypes());
            }
        }
        return this;
    }

    /// Verifies that the effect allows all five colors.
    public AddManaCombinationEffectAssert allowsAllColors() {
        isNotNull();
        var allColors = Set.of(ManaType.WHITE, ManaType.BLUE, ManaType.BLACK, ManaType.RED, ManaType.GREEN);
        if (!actual.allowedTypes().equals(allColors)) {
            failWithMessage(
                    "Expected allowedTypes to be all colors <%s> but was <%s>", allColors, actual.allowedTypes());
        }
        return this;
    }
}
