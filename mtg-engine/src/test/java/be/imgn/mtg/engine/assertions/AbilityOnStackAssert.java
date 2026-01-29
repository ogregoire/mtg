package be.imgn.mtg.engine.assertions;

import org.assertj.core.api.AbstractObjectAssert;

import be.imgn.mtg.engine.ability.Ability;
import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.object.AbilityOnStack;
import be.imgn.mtg.engine.object.TypedObject;

/// Assertion class for AbilityOnStack.
public class AbilityOnStackAssert extends AbstractObjectAssert<AbilityOnStackAssert, AbilityOnStack> {

    protected AbilityOnStackAssert(AbilityOnStack actual) {
        super(actual, AbilityOnStackAssert.class);
    }

    public static AbilityOnStackAssert assertThat(AbilityOnStack actual) {
        return new AbilityOnStackAssert(actual);
    }

    public AbilityOnStackAssert hasOwner(Player expected) {
        isNotNull();
        if (!actual.owner().equals(expected)) {
            failWithMessage("Expected owner to be <%s> but was <%s>", expected, actual.owner());
        }
        return this;
    }

    public AbilityOnStackAssert hasController(Player expected) {
        isNotNull();
        if (!actual.controller().equals(expected)) {
            failWithMessage("Expected controller to be <%s> but was <%s>", expected, actual.controller());
        }
        return this;
    }

    public AbilityOnStackAssert hasAbility(Ability expected) {
        isNotNull();
        if (!actual.ability().equals(expected)) {
            failWithMessage("Expected ability to be <%s> but was <%s>", expected, actual.ability());
        }
        return this;
    }

    public AbilityOnStackAssert hasSource(TypedObject expected) {
        isNotNull();
        if (!actual.source().equals(expected)) {
            failWithMessage("Expected source to be <%s> but was <%s>", expected, actual.source());
        }
        return this;
    }
}
