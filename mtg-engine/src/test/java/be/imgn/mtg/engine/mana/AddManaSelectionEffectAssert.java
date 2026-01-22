package be.imgn.mtg.engine.mana;

import java.util.List;

import org.assertj.core.api.AbstractAssert;

/// Assertion class for AddManaFromSelectionEffect.
public class AddManaSelectionEffectAssert extends AbstractAssert<AddManaSelectionEffectAssert, AddManaSelectionEffect> {

    public AddManaSelectionEffectAssert(AddManaSelectionEffect actual) {
        super(actual, AddManaSelectionEffectAssert.class);
    }

    public static AddManaSelectionEffectAssert assertThat(AddManaSelectionEffect actual) {
        return new AddManaSelectionEffectAssert(actual);
    }

    /// Verifies that the number of options equals the expected value.
    public AddManaSelectionEffectAssert hasOptionCount(int expected) {
        isNotNull();
        if (actual.options().size() != expected) {
            failWithMessage(
                    "Expected option count to be <%d> but was <%d>",
                    expected, actual.options().size());
        }
        return this;
    }

    /// Verifies that the options equal the expected options.
    public AddManaSelectionEffectAssert hasOptions(List<List<ManaType>> expected) {
        isNotNull();
        if (!actual.options().equals(expected)) {
            failWithMessage("Expected options to be <%s> but was <%s>", expected, actual.options());
        }
        return this;
    }

    /// Verifies that the option at the given index equals the expected mana types.
    public AddManaSelectionEffectAssert hasOptionAt(int index, List<ManaType> expected) {
        isNotNull();
        if (index >= actual.options().size()) {
            failWithMessage(
                    "Expected option at index <%d> but only <%d> options exist",
                    index, actual.options().size());
        }
        if (!actual.options().get(index).equals(expected)) {
            failWithMessage(
                    "Expected option at index <%d> to be <%s> but was <%s>",
                    index, expected, actual.options().get(index));
        }
        return this;
    }

    /// Verifies that the option at the given index contains the expected mana types.
    public AddManaSelectionEffectAssert hasOptionAtContaining(int index, ManaType... expected) {
        isNotNull();
        if (index >= actual.options().size()) {
            failWithMessage(
                    "Expected option at index <%d> but only <%d> options exist",
                    index, actual.options().size());
        }
        var option = actual.options().get(index);
        for (var manaType : expected) {
            if (!option.contains(manaType)) {
                failWithMessage("Expected option at index <%d> to contain <%s> but was <%s>", index, manaType, option);
            }
        }
        return this;
    }
}
