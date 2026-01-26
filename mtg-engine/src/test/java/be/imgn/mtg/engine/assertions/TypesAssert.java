package be.imgn.mtg.engine.assertions;

import org.assertj.core.api.AbstractObjectAssert;

import be.imgn.mtg.engine.characteristics.Type;
import be.imgn.mtg.engine.characteristics.Types;

/// Assertion class for Types.
public class TypesAssert extends AbstractObjectAssert<TypesAssert, Types> {

    protected TypesAssert(Types actual) {
        super(actual, TypesAssert.class);
    }

    public static TypesAssert assertThat(Types actual) {
        return new TypesAssert(actual);
    }

    public TypesAssert isEmpty() {
        isNotNull();
        if (!actual.isEmpty()) {
            failWithMessage("Expected types to be empty but was <%s>", actual);
        }
        return this;
    }

    public TypesAssert isNotEmpty() {
        isNotNull();
        if (actual.isEmpty()) {
            failWithMessage("Expected types not to be empty");
        }
        return this;
    }

    public TypesAssert hasCount(int expected) {
        isNotNull();
        if (actual.count() != expected) {
            failWithMessage("Expected type count to be <%d> but was <%d>", expected, actual.count());
        }
        return this;
    }

    public TypesAssert contains(Type type) {
        isNotNull();
        if (!actual.contains(type)) {
            failWithMessage("Expected types to contain <%s> but was <%s>", type, actual);
        }
        return this;
    }

    public TypesAssert doesNotContain(Type type) {
        isNotNull();
        if (actual.contains(type)) {
            failWithMessage("Expected types not to contain <%s> but was <%s>", type, actual);
        }
        return this;
    }

    public TypesAssert containsExactly(Type... types) {
        isNotNull();
        if (actual.count() != types.length) {
            failWithMessage("Expected exactly <%d> types but had <%d>: <%s>", types.length, actual.count(), actual);
        }
        for (var type : types) {
            if (!actual.contains(type)) {
                failWithMessage("Expected types to contain <%s> but was <%s>", type, actual);
            }
        }
        return this;
    }

    public TypesAssert isArtifact() {
        return contains(Type.ARTIFACT);
    }

    public TypesAssert isCreature() {
        return contains(Type.CREATURE);
    }

    public TypesAssert isEnchantment() {
        return contains(Type.ENCHANTMENT);
    }

    public TypesAssert isInstant() {
        return contains(Type.INSTANT);
    }

    public TypesAssert isLand() {
        return contains(Type.LAND);
    }

    public TypesAssert isPlaneswalker() {
        return contains(Type.PLANESWALKER);
    }

    public TypesAssert isSorcery() {
        return contains(Type.SORCERY);
    }

    public TypesAssert isPermanentType() {
        isNotNull();
        if (!actual.isPermanentType()) {
            failWithMessage("Expected to be a permanent type but was <%s>", actual);
        }
        return this;
    }

    public TypesAssert isSpellType() {
        isNotNull();
        if (!actual.isSpellType()) {
            failWithMessage("Expected to be a spell type but was <%s>", actual);
        }
        return this;
    }
}
