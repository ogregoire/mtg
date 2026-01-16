package be.imgn.mtg.engine.characteristics.internal;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import be.imgn.mtg.engine.characteristics.Subtype;
import be.imgn.mtg.engine.characteristics.Subtypes;

/// Default implementation of Subtypes.
public final class DefaultSubtypes extends AbstractCharacteristics<Subtype, Subtypes> implements Subtypes {

    private static final DefaultSubtypes EMPTY = new DefaultSubtypes(Set.of());

    private DefaultSubtypes(Set<Subtype> elements) {
        super(elements);
    }

    public static Subtypes empty() {
        return EMPTY;
    }

    public static Subtypes of(Subtype... subtypes) {
        if (subtypes.length == 0) {
            return EMPTY;
        }
        var set = new LinkedHashSet<>(Arrays.asList(subtypes));
        return new DefaultSubtypes(Set.copyOf(set));
    }

    public static Subtypes.Builder builder() {
        return new Builder();
    }

    @Override
    public Subtypes.Builder toBuilder() {
        return new Builder(elements);
    }

    static final class Builder extends AbstractCharacteristics.Builder<Subtype, Subtypes, Subtypes.Builder>
            implements Subtypes.Builder {

        Builder() {
            super(new LinkedHashSet<>(), () -> EMPTY, set -> new DefaultSubtypes(Set.copyOf(set)));
        }

        Builder(Set<Subtype> initial) {
            super(new LinkedHashSet<>(initial), () -> EMPTY, set -> new DefaultSubtypes(Set.copyOf(set)));
        }
    }
}
