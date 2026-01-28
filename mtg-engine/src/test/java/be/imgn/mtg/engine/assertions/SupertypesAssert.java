package be.imgn.mtg.engine.assertions;

import org.assertj.core.api.AbstractObjectAssert;

import be.imgn.mtg.engine.characteristics.Supertype;
import be.imgn.mtg.engine.characteristics.Supertypes;

/// Assertion class for Supertypes.
public class SupertypesAssert extends AbstractObjectAssert<SupertypesAssert, Supertypes> {

    protected SupertypesAssert(Supertypes actual) {
        super(actual, SupertypesAssert.class);
    }

    public static SupertypesAssert assertThat(Supertypes actual) {
        return new SupertypesAssert(actual);
    }

    public SupertypesAssert isEmpty() {
        isNotNull();
        if (!actual.isEmpty()) {
            failWithMessage("Expected supertypes to be empty but was <%s>", actual);
        }
        return this;
    }

    public SupertypesAssert isNotEmpty() {
        isNotNull();
        if (actual.isEmpty()) {
            failWithMessage("Expected supertypes not to be empty");
        }
        return this;
    }

    public SupertypesAssert hasCount(int expected) {
        isNotNull();
        if (actual.count() != expected) {
            failWithMessage("Expected supertype count to be <%d> but was <%d>", expected, actual.count());
        }
        return this;
    }

    public SupertypesAssert contains(Supertype supertype) {
        isNotNull();
        if (!actual.contains(supertype)) {
            failWithMessage("Expected supertypes to contain <%s> but was <%s>", supertype, actual);
        }
        return this;
    }

    public SupertypesAssert doesNotContain(Supertype supertype) {
        isNotNull();
        if (actual.contains(supertype)) {
            failWithMessage("Expected supertypes not to contain <%s> but was <%s>", supertype, actual);
        }
        return this;
    }

    public SupertypesAssert isBasic() {
        return contains(Supertype.BASIC);
    }

    public SupertypesAssert isLegendary() {
        return contains(Supertype.LEGENDARY);
    }

    public SupertypesAssert isSnow() {
        return contains(Supertype.SNOW);
    }

    public SupertypesAssert isWorld() {
        return contains(Supertype.WORLD);
    }

    public SupertypesAssert containsExactly(Supertype... supertypes) {
        isNotNull();
        if (actual.count() != supertypes.length) {
            failWithMessage(
                    "Expected exactly <%d> supertypes but had <%d>: <%s>", supertypes.length, actual.count(), actual);
        }
        for (var supertype : supertypes) {
            if (!actual.contains(supertype)) {
                failWithMessage("Expected supertypes to contain <%s> but was <%s>", supertype, actual);
            }
        }
        return this;
    }
}
