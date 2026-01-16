package be.imgn.mtg.engine.characteristics.internal;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

import be.imgn.mtg.engine.characteristics.Type;
import be.imgn.mtg.engine.characteristics.Types;

/// Default implementation of Types.
public final class DefaultTypes extends AbstractCharacteristics<Type, Types> implements Types {

    private static final DefaultTypes EMPTY = new DefaultTypes(EnumSet.noneOf(Type.class));

    private DefaultTypes(Set<Type> elements) {
        super(elements);
    }

    public static Types empty() {
        return EMPTY;
    }

    public static Types of(Type... types) {
        if (types.length == 0) {
            return EMPTY;
        }
        var set = EnumSet.noneOf(Type.class);
        set.addAll(Arrays.asList(types));
        return new DefaultTypes(set);
    }

    public static Types.Builder builder() {
        return new Builder();
    }

    @Override
    public Types.Builder toBuilder() {
        return new Builder(elements);
    }

    static final class Builder extends AbstractCharacteristics.Builder<Type, Types, Types.Builder>
            implements Types.Builder {

        Builder() {
            super(EnumSet.noneOf(Type.class), () -> EMPTY, set -> new DefaultTypes(EnumSet.copyOf(set)));
        }

        Builder(Set<Type> initial) {
            super(EnumSet.copyOf(initial), () -> EMPTY, set -> new DefaultTypes(EnumSet.copyOf(set)));
        }
    }
}
