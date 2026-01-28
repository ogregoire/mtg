package be.imgn.mtg.engine.characteristics.internal;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collector;

import be.imgn.mtg.engine.characteristics.Supertype;
import be.imgn.mtg.engine.characteristics.Supertypes;

/// Default implementation of Supertypes.
public final class DefaultSupertypes extends AbstractCharacteristics<Supertype, Supertypes> implements Supertypes {

    private static final DefaultSupertypes EMPTY = new DefaultSupertypes(EnumSet.noneOf(Supertype.class));

    private DefaultSupertypes(Set<Supertype> elements) {
        super(elements);
    }

    public static Supertypes empty() {
        return EMPTY;
    }

    public static Supertypes of(Supertype... supertypes) {
        if (supertypes.length == 0) {
            return EMPTY;
        }
        var set = EnumSet.noneOf(Supertype.class);
        set.addAll(Arrays.asList(supertypes));
        return new DefaultSupertypes(set);
    }

    public static Supertypes.Builder builder() {
        return new Builder();
    }

    /// Returns an optimized Collector for Supertypes using EnumSet.
    public static Collector<Supertype, ?, Supertypes> collector() {
        return Collector.of(
                () -> EnumSet.noneOf(Supertype.class),
                Set::add,
                (s1, s2) -> {
                    s1.addAll(s2);
                    return s1;
                },
                set -> set.isEmpty() ? EMPTY : new DefaultSupertypes(EnumSet.copyOf(set)),
                Collector.Characteristics.UNORDERED);
    }

    @Override
    public Supertypes.Builder toBuilder() {
        return new Builder(elements);
    }

    static final class Builder extends AbstractCharacteristics.Builder<Supertype, Supertypes, Supertypes.Builder>
            implements Supertypes.Builder {

        Builder() {
            super(EnumSet.noneOf(Supertype.class), () -> EMPTY, set -> new DefaultSupertypes(EnumSet.copyOf(set)));
        }

        Builder(Set<Supertype> initial) {
            super(EnumSet.copyOf(initial), () -> EMPTY, set -> new DefaultSupertypes(EnumSet.copyOf(set)));
        }
    }
}
