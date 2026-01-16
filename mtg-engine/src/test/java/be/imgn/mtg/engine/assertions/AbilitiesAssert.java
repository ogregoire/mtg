package be.imgn.mtg.engine.assertions;

import org.assertj.core.api.AbstractObjectAssert;

import be.imgn.mtg.engine.characteristics.Abilities;
import be.imgn.mtg.engine.characteristics.Ability;

/// Assertion class for Abilities.
public class AbilitiesAssert extends AbstractObjectAssert<AbilitiesAssert, Abilities> {

    protected AbilitiesAssert(Abilities actual) {
        super(actual, AbilitiesAssert.class);
    }

    public static AbilitiesAssert assertThat(Abilities actual) {
        return new AbilitiesAssert(actual);
    }

    public AbilitiesAssert isEmpty() {
        isNotNull();
        if (!actual.isEmpty()) {
            failWithMessage("Expected abilities to be empty but was <%s>", actual);
        }
        return this;
    }

    public AbilitiesAssert isNotEmpty() {
        isNotNull();
        if (actual.isEmpty()) {
            failWithMessage("Expected abilities not to be empty");
        }
        return this;
    }

    public AbilitiesAssert hasCount(int expected) {
        isNotNull();
        if (actual.count() != expected) {
            failWithMessage("Expected ability count to be <%d> but was <%d>", expected, actual.count());
        }
        return this;
    }

    public AbilitiesAssert contains(Ability ability) {
        isNotNull();
        if (!actual.contains(ability)) {
            failWithMessage("Expected abilities to contain <%s> but was <%s>", ability, actual);
        }
        return this;
    }

    public AbilitiesAssert doesNotContain(Ability ability) {
        isNotNull();
        if (actual.contains(ability)) {
            failWithMessage("Expected abilities not to contain <%s> but was <%s>", ability, actual);
        }
        return this;
    }
}
