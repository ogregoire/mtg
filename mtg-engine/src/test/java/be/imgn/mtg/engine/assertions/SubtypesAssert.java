package be.imgn.mtg.engine.assertions;

import org.assertj.core.api.AbstractObjectAssert;

import be.imgn.mtg.engine.characteristics.Subtype;
import be.imgn.mtg.engine.characteristics.Subtypes;

/// Assertion class for Subtypes.
public class SubtypesAssert extends AbstractObjectAssert<SubtypesAssert, Subtypes> {

    protected SubtypesAssert(Subtypes actual) {
        super(actual, SubtypesAssert.class);
    }

    public static SubtypesAssert assertThat(Subtypes actual) {
        return new SubtypesAssert(actual);
    }

    public SubtypesAssert isEmpty() {
        isNotNull();
        if (!actual.isEmpty()) {
            failWithMessage("Expected subtypes to be empty but was <%s>", actual);
        }
        return this;
    }

    public SubtypesAssert isNotEmpty() {
        isNotNull();
        if (actual.isEmpty()) {
            failWithMessage("Expected subtypes not to be empty");
        }
        return this;
    }

    public SubtypesAssert hasCount(int expected) {
        isNotNull();
        if (actual.count() != expected) {
            failWithMessage("Expected subtype count to be <%d> but was <%d>", expected, actual.count());
        }
        return this;
    }

    public SubtypesAssert contains(Subtype subtype) {
        isNotNull();
        if (!actual.contains(subtype)) {
            failWithMessage("Expected subtypes to contain <%s> but was <%s>", subtype, actual);
        }
        return this;
    }

    public SubtypesAssert doesNotContain(Subtype subtype) {
        isNotNull();
        if (actual.contains(subtype)) {
            failWithMessage("Expected subtypes not to contain <%s> but was <%s>", subtype, actual);
        }
        return this;
    }

    public SubtypesAssert containsExactly(Subtype... subtypes) {
        isNotNull();
        if (actual.count() != subtypes.length) {
            failWithMessage(
                    "Expected exactly <%d> subtypes but had <%d>: <%s>", subtypes.length, actual.count(), actual);
        }
        for (Subtype subtype : subtypes) {
            if (!actual.contains(subtype)) {
                failWithMessage("Expected subtypes to contain <%s> but was <%s>", subtype, actual);
            }
        }
        return this;
    }
}
