package be.imgn.mtg.engine.assertions;

import be.imgn.mtg.engine.ability.Ability;
import be.imgn.mtg.engine.object.AbilityOnStack;
import be.imgn.mtg.engine.object.GameObject;

/// Assertion class for AbilityOnStack.
public class AbilityOnStackAssert extends AbstractGameObjectAssert<AbilityOnStackAssert, AbilityOnStack> {

    protected AbilityOnStackAssert(AbilityOnStack actual) {
        super(actual, AbilityOnStackAssert.class);
    }

    public static AbilityOnStackAssert assertThat(AbilityOnStack actual) {
        return new AbilityOnStackAssert(actual);
    }

    public AbilityOnStackAssert hasAbility(Ability expected) {
        isNotNull();
        if (!actual.ability().equals(expected)) {
            failWithMessage("Expected ability to be <%s> but was <%s>", expected, actual.ability());
        }
        return this;
    }

    public AbilityOnStackAssert hasSource(GameObject expected) {
        isNotNull();
        if (!actual.source().equals(expected)) {
            failWithMessage("Expected source to be <%s> but was <%s>", expected, actual.source());
        }
        return this;
    }
}
